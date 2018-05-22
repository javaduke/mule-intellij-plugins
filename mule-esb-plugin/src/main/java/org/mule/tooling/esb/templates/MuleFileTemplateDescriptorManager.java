package org.mule.tooling.esb.templates;


import com.intellij.icons.AllIcons;
import com.intellij.ide.fileTemplates.FileTemplateDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptorFactory;
import icons.MavenIcons;
import org.mule.tooling.esb.util.MuleIcons;

public class MuleFileTemplateDescriptorManager implements FileTemplateGroupDescriptorFactory {
    public static final String MULE_CONFIGURATION_FILE = "Mule Config";
    public static final String MULE_DOMAIN_CONFIGURATION_FILE = "Mule Domain Config";
    public static final String DATA_WEAVE_FILE = "Data Weave";
    public static final String MEL_FILE = "Mel File";
    public static final String MULE_MAVEN_PROJECT = "Mule Maven Project";
    public static final String MULE_MAVEN_MODULE = "Mule Maven Module";
    public static final String MULE_DOMAIN_MAVEN_PROJECT = "Mule Domain Maven Project";
    public static final String MULE_DEPLOY_PROPERTIES = "Mule Deploy Properties";
    public static final String MULE_DOMAIN_DEPLOY_PROPERTIES = "Mule Domain Deploy Properties";
    public static final String LOG4J2 = "log4j2";
    public static final String LOG4J2_TEST = "log4j2-test";
    public static final String MUNIT_FILE = "MUnit Config";
    public static final String RAML_FILE = "RAML File";

    public static final String MULE4_CONFIGURATION_FILE = "Mule 4 Config";
    public static final String MULE4_ARTIFACT_FILE = "Mule 4 Artifact";
    public static final String MULE4_LOG4J2 = "mule4-log4j2";
    public static final String MULE4_LOG4J2_TEST = "mule4-log4j2-test";
    public static final String MULE4_MAVEN_PROJECT = "Mule 4 Maven Project";
    public static final String MULE4_ARTIFACT_PROPERTIES = "Mule 4 Artifact Properties";

    @Override
    public FileTemplateGroupDescriptor getFileTemplatesDescriptor() {
        final FileTemplateGroupDescriptor group = new FileTemplateGroupDescriptor("Mule", MuleIcons.MuleIcon);
        group.addTemplate(new FileTemplateDescriptor(MULE_CONFIGURATION_FILE, MuleIcons.MuleFileType));
        group.addTemplate(new FileTemplateDescriptor(MULE_DOMAIN_CONFIGURATION_FILE, MuleIcons.MuleFileType));
        group.addTemplate(new FileTemplateDescriptor(DATA_WEAVE_FILE, MuleIcons.DataFileType));
        group.addTemplate(new FileTemplateDescriptor(MULE_MAVEN_PROJECT, MavenIcons.MavenLogo));
        group.addTemplate(new FileTemplateDescriptor(MULE_MAVEN_MODULE, MavenIcons.MavenLogo));
        group.addTemplate(new FileTemplateDescriptor(MULE_DOMAIN_MAVEN_PROJECT, MavenIcons.MavenLogo));
        group.addTemplate(new FileTemplateDescriptor(MUNIT_FILE, MuleIcons.MUnitFileType));
        group.addTemplate(new FileTemplateDescriptor(RAML_FILE, MuleIcons.RamlFileType));

        //Mule 4 Support
        group.addTemplate(new FileTemplateDescriptor(MULE4_CONFIGURATION_FILE, MuleIcons.MuleFileType));
        group.addTemplate(new FileTemplateDescriptor(MULE4_ARTIFACT_FILE, AllIcons.FileTypes.Json));
        group.addTemplate(new FileTemplateDescriptor(MULE4_MAVEN_PROJECT, MavenIcons.MavenLogo));

        return group;
    }
}
