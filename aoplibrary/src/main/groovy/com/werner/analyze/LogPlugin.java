package com.werner.analyze;

import com.android.build.gradle.AppExtension;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.util.Collections;

/**
 * @author CWQ
 * @date 2020/6/29
 */
public class LogPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        AppExtension appExtension = (AppExtension)project.getProperties().get("android");
        appExtension.registerTransform(new LogTransform(project), Collections.EMPTY_LIST);
    }
}
