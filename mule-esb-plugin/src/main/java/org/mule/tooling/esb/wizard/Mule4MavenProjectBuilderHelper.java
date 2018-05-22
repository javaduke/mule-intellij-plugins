package org.mule.tooling.esb.wizard;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.util.EditorHelper;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.model.MavenConstants;
import org.jetbrains.idea.maven.model.MavenId;
import org.jetbrains.idea.maven.utils.MavenUtil;
import org.mule.tooling.esb.templates.MuleFileTemplateDescriptorManager;

import java.io.IOException;
import java.util.Properties;

public class Mule4MavenProjectBuilderHelper
{

    public void configure(final Project project, final MavenId projectId, final String muleVersion, final VirtualFile root, @Nullable MavenId parentId)
    {
        try
        {
            //Create mule folders.
            final VirtualFile appDirectory = VfsUtil.createDirectories(root.getPath() + "/src/main/mule");
            final VirtualFile resources = VfsUtil.createDirectories(root.getPath() + "/src/main/resources");
            final VirtualFile api = VfsUtil.createDirectories(root.getPath() + "/src/main/resources/api");
            createLog4J(project, projectId, resources);
            final VirtualFile muleConfigFile = createMuleConfigFile(project, projectId, appDirectory);

            createMuleArtifactFile(project, projectId, root);
            createMuleArtifactPropertyFile(project, resources);

            //MUnit support
            VfsUtil.createDirectories(root.getPath() + "/src/test/munit");
            final VirtualFile testResources = VfsUtil.createDirectories(root.getPath() + "/src/test/resources");
            createLog4JTest(project, projectId, testResources);

            if (parentId == null)
                createPomFile(project, projectId, muleVersion, root);
            else
                createModulePomFile(project, projectId, root, parentId);

            // execute when current dialog is closed (e.g. Project Structure)
            MavenUtil.invokeLater(project, ModalityState.NON_MODAL, () -> EditorHelper.openInEditor(getPsiFile(project, muleConfigFile)));

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private static PsiFile getPsiFile(Project project, VirtualFile pom)
    {
        return PsiManager.getInstance(project).findFile(pom);
    }

    private VirtualFile createMuleArtifactPropertyFile(final Project project, final VirtualFile resourcesDirectory)
    {
        return new WriteCommandAction<VirtualFile>(project, "Create Mule Artifact Properties File", PsiFile.EMPTY_ARRAY)
        {
            @Override
            protected void run(@NotNull Result<VirtualFile> result) throws Throwable
            {
                try
                {
                    VirtualFile configFile = resourcesDirectory.findOrCreateChildData(this, "mule-artifact.properties");
                    final Properties templateProps = new Properties();
                    final FileTemplateManager manager = FileTemplateManager.getInstance(project);
                    final FileTemplate template = manager.getInternalTemplate(MuleFileTemplateDescriptorManager.MULE4_ARTIFACT_PROPERTIES);
                    final Properties defaultProperties = manager.getDefaultProperties();
                    defaultProperties.putAll(templateProps);
                    final String text = template.getText(defaultProperties);
                    VfsUtil.saveText(configFile, text);
                    result.setResult(configFile);
                }
                catch (IOException e)
                {
                    showError(project, e);
                }
            }
        }.execute().getResultObject();
    }

    private VirtualFile createMuleConfigFile(final Project project, final MavenId projectId, final VirtualFile appDirectory)
    {
        return new WriteCommandAction<VirtualFile>(project, "Create Mule Config File", PsiFile.EMPTY_ARRAY)
        {
            @Override
            protected void run(@NotNull Result<VirtualFile> result) throws Throwable
            {

                try
                {
                    VirtualFile configFile = appDirectory.findOrCreateChildData(this, projectId.getArtifactId() + ".xml");
                    final Properties templateProps = new Properties();
                    templateProps.setProperty("NAME", projectId.getArtifactId());
                    final FileTemplateManager manager = FileTemplateManager.getInstance(project);
                    final FileTemplate template = manager.getInternalTemplate(MuleFileTemplateDescriptorManager.MULE4_CONFIGURATION_FILE);
                    final Properties defaultProperties = manager.getDefaultProperties();
                    defaultProperties.putAll(templateProps);
                    final String text = template.getText(defaultProperties);
                    VfsUtil.saveText(configFile, text);
                    result.setResult(configFile);
                }
                catch (IOException e)
                {
                    showError(project, e);
                }
            }
        }.execute().getResultObject();
    }

    private VirtualFile createLog4J(final Project project, final MavenId projectId, final VirtualFile appDirectory)
    {
        return new WriteCommandAction<VirtualFile>(project, "Create Log4J File", PsiFile.EMPTY_ARRAY)
        {
            @Override
            protected void run(@NotNull Result<VirtualFile> result) throws Throwable
            {

                try
                {
                    VirtualFile configFile = appDirectory.findOrCreateChildData(this, "log4j2.xml");
                    final Properties templateProps = new Properties();
                    templateProps.setProperty("FILE_NAME", "${sys:mule.home}${sys:file.separator}logs${sys:file.separator}" + projectId.getArtifactId().toLowerCase() + ".log");
                    templateProps.setProperty("FILE_PATTERN", "${sys:mule.home}${sys:file.separator}logs${sys:file.separator}" + projectId.getArtifactId().toLowerCase() + "-%i.log");
                    final FileTemplateManager manager = FileTemplateManager.getInstance(project);
                    final FileTemplate template = manager.getInternalTemplate(MuleFileTemplateDescriptorManager.MULE4_LOG4J2);
                    final Properties defaultProperties = manager.getDefaultProperties();
                    defaultProperties.putAll(templateProps);
                    final String text = template.getText(defaultProperties);
                    VfsUtil.saveText(configFile, text);
                    result.setResult(configFile);
                }
                catch (IOException e)
                {
                    showError(project, e);
                }
            }
        }.execute().getResultObject();
    }

    private VirtualFile createLog4JTest(final Project project, final MavenId projectId, final VirtualFile appDirectory)
    {
        return new WriteCommandAction<VirtualFile>(project, "Create Log4J Test File", PsiFile.EMPTY_ARRAY)
        {
            @Override
            protected void run(@NotNull Result<VirtualFile> result) throws Throwable
            {

                try
                {
                    VirtualFile configFile = appDirectory.findOrCreateChildData(this, "log4j2-test.xml");
                    final FileTemplateManager manager = FileTemplateManager.getInstance(project);
                    final FileTemplate template = manager.getInternalTemplate(MuleFileTemplateDescriptorManager.MULE4_LOG4J2_TEST);
                    final Properties defaultProperties = manager.getDefaultProperties();
                    final String text = template.getText(defaultProperties);
                    VfsUtil.saveText(configFile, text);
                    result.setResult(configFile);
                }
                catch (IOException e)
                {
                    showError(project, e);
                }
            }
        }.execute().getResultObject();
    }

    private VirtualFile createMuleArtifactFile(final Project project, final MavenId projectId, final VirtualFile appDirectory)
    {
        return new WriteCommandAction<VirtualFile>(project, "Create Mule Artifact File", PsiFile.EMPTY_ARRAY)
        {
            @Override
            protected void run(@NotNull Result<VirtualFile> result) throws Throwable
            {

                try
                {
                    VirtualFile configFile = appDirectory.findOrCreateChildData(this, "mule-artifact.json");
                    final Properties templateProps = new Properties();
                    templateProps.setProperty("NAME", projectId.getArtifactId());
                    final FileTemplateManager manager = FileTemplateManager.getInstance(project);
                    final FileTemplate template = manager.getInternalTemplate(MuleFileTemplateDescriptorManager.MULE4_ARTIFACT_FILE);
                    final Properties defaultProperties = manager.getDefaultProperties();
                    defaultProperties.putAll(templateProps);
                    final String text = template.getText(defaultProperties);
                    VfsUtil.saveText(configFile, text);
                    result.setResult(configFile);
                }
                catch (IOException e)
                {
                    showError(project, e);
                }
            }
        }.execute().getResultObject();
    }

    private static void showError(Project project, Throwable e)
    {
        MavenUtil.showError(project, "Failed to create a Mule project", e);
    }

    private VirtualFile createModulePomFile(final Project project, final MavenId projectId, final VirtualFile root, final MavenId parentId)
    {
        return new WriteCommandAction<VirtualFile>(project, "Create Mule Project", PsiFile.EMPTY_ARRAY)
        {
            @Override
            protected void run(@NotNull Result<VirtualFile> result) throws Throwable
            {
                try
                {
                    VirtualFile pomFile = root.findOrCreateChildData(this, MavenConstants.POM_XML);
                    final Properties templateProps = new Properties();
                    templateProps.setProperty("GROUP_ID", parentId.getGroupId());
                    templateProps.setProperty("ARTIFACT_ID", projectId.getArtifactId());
                    templateProps.setProperty("PARENT_ID", parentId.getArtifactId());
                    templateProps.setProperty("VERSION", parentId.getVersion());

                    final FileTemplateManager manager = FileTemplateManager.getInstance(project);
                    final FileTemplate template = manager.getInternalTemplate(MuleFileTemplateDescriptorManager.MULE_MAVEN_MODULE);
                    final Properties defaultProperties = manager.getDefaultProperties();
                    defaultProperties.putAll(templateProps);
                    final String text = template.getText(defaultProperties);
                    VfsUtil.saveText(pomFile, text);
                    result.setResult(pomFile);
                }
                catch (IOException e)
                {
                    showError(project, e);
                }
            }
        }.execute().getResultObject();
    }

    private VirtualFile createPomFile(final Project project, final MavenId projectId, final String muleVersion, final VirtualFile root)
    {
        return new WriteCommandAction<VirtualFile>(project, "Create Mule Project", PsiFile.EMPTY_ARRAY)
        {
            @Override
            protected void run(@NotNull Result<VirtualFile> result) throws Throwable
            {

                try
                {
                    VirtualFile pomFile = root.findOrCreateChildData(this, MavenConstants.POM_XML);
                    final Properties templateProps = new Properties();
                    templateProps.setProperty("GROUP_ID", projectId.getGroupId());
                    templateProps.setProperty("ARTIFACT_ID", projectId.getArtifactId());
                    templateProps.setProperty("VERSION", projectId.getVersion());
                    templateProps.setProperty("MULE_VERSION", muleVersion);
                    final FileTemplateManager manager = FileTemplateManager.getInstance(project);
                    final FileTemplate template = manager.getInternalTemplate(MuleFileTemplateDescriptorManager.MULE4_MAVEN_PROJECT);
                    final Properties defaultProperties = manager.getDefaultProperties();
                    defaultProperties.putAll(templateProps);
                    final String text = template.getText(defaultProperties);
                    VfsUtil.saveText(pomFile, text);
                    result.setResult(pomFile);
                }
                catch (IOException e)
                {
                    showError(project, e);
                }
            }
        }.execute().getResultObject();
    }
}
