package com.werner.analyze;

import com.android.build.api.transform.Context;
import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformOutputProvider;
import com.android.build.gradle.internal.pipeline.TransformManager;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.gradle.api.Project;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import static org.objectweb.asm.ClassReader.EXPAND_FRAMES;

/**
 * @author CWQ
 * @date 2020/6/29
 */
public class LogTransform extends Transform {

    private Project project;

    LogTransform(Project project) {
        this.project = project;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS;
    }

    @Override
    public Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT;
    }

    @Override
    public boolean isIncremental() {
        return false;
    }

    @Override
    public void transform(Context context, Collection<TransformInput> inputs, Collection<TransformInput> referencedInputs, TransformOutputProvider outputProvider, boolean isIncremental) throws IOException, TransformException, InterruptedException {
        System.out.println("//===============TracePlugin visit start===============//");
        //删除之前的输出
        if (outputProvider != null) {
            outputProvider.deleteAll();
        }
        //遍历inputs里的TransformInput
        for (TransformInput input : inputs) {
            //遍历input里边的DirectoryInput
            for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
                //是否是目录
                if (directoryInput.getFile().isDirectory()) {
                    for (File file : com.android.utils.FileUtils.getAllFiles(directoryInput.getFile())) {
                        String name = file.getName();
                        System.out.println("name:"+name);
                        //这里进行我们的处理
                        if (name.endsWith(".class") && !name.startsWith("R\\$") &&
                                !"R.class".equals(name) && !"BuildConfig.class".equals(name)) {
                            FileInputStream is = new FileInputStream(file);
                            ClassReader classReader = new ClassReader(is);
                            ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);
                            String className = name.split(".class")[0];
                            ClassVisitor cv = new TraceVisitor(className, classWriter);
                            classReader.accept(cv, EXPAND_FRAMES);
                            byte[] code = classWriter.toByteArray();
                            FileOutputStream fos = new FileOutputStream(
                                    file.getParentFile().getAbsolutePath() + File.separator + name);
                            fos.write(code);
                            fos.close();
                        }
                    }
                }

                //处理完输入文件之后，要把输出给下一个任务
                File dest = outputProvider.getContentLocation(directoryInput.getName(),
                        directoryInput.getContentTypes(), directoryInput.getScopes(),
                        Format.DIRECTORY);
                FileUtils.copyDirectory(directoryInput.getFile(), dest);
            }


            for (JarInput jarInput : input.getJarInputs()) {
                /**
                 * 重名名输出文件,因为可能同名,会覆盖
                 */
                String jarName = jarInput.getName();
                String md5Name = DigestUtils.md5Hex(jarInput.getFile().getAbsolutePath());
                if (jarName.endsWith(".jar")) {
                    jarName = jarName.substring(0, jarName.length() - 4);
                }

                File tmpFile = null;
                if (jarInput.getFile().getAbsolutePath().endsWith(".jar")) {
                    JarFile jarFile = new JarFile(jarInput.getFile());
                    Enumeration enumeration = jarFile.entries();
                    tmpFile = new File(jarInput.getFile().getParent() + File.separator + "classes_trace.jar");
                    //避免上次的缓存被重复插入
                    if (tmpFile.exists()) {
                        tmpFile.delete();
                    }
                    JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(tmpFile));
                    //用于保存
                    ArrayList<String> processorList = new ArrayList<>();
                    while (enumeration.hasMoreElements()) {
                        JarEntry jarEntry = (JarEntry) enumeration.nextElement();
                        String entryName = jarEntry.getName();
                        ZipEntry zipEntry = new ZipEntry(entryName);
                        //println "MeetyouCost entryName :" + entryName
                        InputStream inputStream = jarFile.getInputStream(jarEntry);
                        //如果是inject文件就跳过

                        //插桩class
                        if (entryName.endsWith(".class") && !entryName.contains("R\\$") &&
                                !entryName.contains("R.class") && !entryName.contains("BuildConfig.class")) {
                            //class文件处理
                            jarOutputStream.putNextEntry(zipEntry);
                            ClassReader classReader = new ClassReader(IOUtils.toByteArray(inputStream));
                            ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);
                            String className = entryName.split(".class")[0];
                            ClassVisitor cv = new TraceVisitor(className, classWriter);
                            classReader.accept(cv, EXPAND_FRAMES);
                            byte[] code = classWriter.toByteArray();
                            jarOutputStream.write(code);

                        } else if (entryName.contains("META-INF/services/javax.annotation.processing.Processor")) {
                            if (!processorList.contains(entryName)) {
                                processorList.add(entryName);
                                jarOutputStream.putNextEntry(zipEntry);
                                jarOutputStream.write(IOUtils.toByteArray(inputStream));
                            } else {
                                System.out.println("duplicate entry:" + entryName);
                            }
                        } else {
                            jarOutputStream.putNextEntry(zipEntry);
                            jarOutputStream.write(IOUtils.toByteArray(inputStream));
                        }

                        jarOutputStream.closeEntry();
                    }
                    //结束
                    jarOutputStream.close();
                    jarFile.close();
                }

                File dest = outputProvider.getContentLocation(jarName + md5Name,
                        jarInput.getContentTypes(), jarInput.getScopes(), Format.JAR);
                if (tmpFile == null) {
                    FileUtils.copyFile(jarInput.getFile(), dest);
                } else {
                    FileUtils.copyFile(tmpFile, dest);
                    tmpFile.delete();
                }
            }
        }
        System.out.println("//===============TracePlugin visit end===============//");
    }
}
