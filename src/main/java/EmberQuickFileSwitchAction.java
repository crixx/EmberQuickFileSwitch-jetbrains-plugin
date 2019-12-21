import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.TestSourcesFilter;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.intellij.psi.search.FilenameIndex.getVirtualFilesByName;
import static com.intellij.psi.search.GlobalSearchScope.moduleWithDependenciesAndLibrariesScope;

public class EmberQuickFileSwitchAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent event) {

        VirtualFile currentFile = (VirtualFile) event.getDataContext().getData("virtualFile");
        if (currentFile == null) return;

        Project currentProject = event.getProject();
        if (currentProject == null) return;

        com.intellij.openapi.module.Module module = ModuleUtilCore.findModuleForFile(currentFile, currentProject);
        if(module == null) return;

        String currentFileNameWithoutExtension = currentFile.getNameWithoutExtension();
        currentFileNameWithoutExtension = currentFileNameWithoutExtension.replace("-test", "");

        GlobalSearchScope scope = moduleWithDependenciesAndLibrariesScope(module, true);
        ArrayList<VirtualFile> files = new ArrayList<>();
        files.addAll(getVirtualFilesByName(currentProject, currentFileNameWithoutExtension + ".js", scope));
        files.addAll(getVirtualFilesByName(currentProject, currentFileNameWithoutExtension + ".html", scope));
        files.addAll(getVirtualFilesByName(currentProject, currentFileNameWithoutExtension + ".hbs", scope));
        files.addAll(getVirtualFilesByName(currentProject, currentFileNameWithoutExtension + "-test.js", scope));

        JBPopup popup = JBPopupFactory.getInstance().createListPopup(new FileSelectionListPopupStep("Related Files", files, currentProject));
        popup.showInFocusCenter();
    }

    private class FileSelectionListPopupStep extends BaseListPopupStep<VirtualFile> {
        private List<String> fileTypes = Arrays.asList(
                "adapters",
                "components",
                "controllers",
                "helpers",
                "models",
                "routes",
                "services",
                "templates",
                "tests/unit",
                "tests/acceptance",
                "tests/integration"
        );

        private Pattern pattern = Pattern.compile("/(" + String.join("|", fileTypes) + ")/");

        Project project;
        public FileSelectionListPopupStep(@Nullable String title, @NotNull List<VirtualFile> values, Project project) {
            super(title, values);
            this.project = project;
        }

        @Nullable
        @Override
        public PopupStep onChosen(VirtualFile selectedFile, boolean finalChoice) {
            if(finalChoice){
                this.openFile(selectedFile);
            }
            return super.onChosen(selectedFile, finalChoice);
        }

        @NotNull
        @Override
        public String getTextFor(VirtualFile file) {
            return String.format("%s - %s: %s", file.getNameWithoutExtension(), this.getType(file), file.getCanonicalPath());
        }

        @Nullable
        @Override
        public Color getBackgroundFor(VirtualFile file) {
//            return FileColorManager.getInstance(project).getFileColor(file);
            return Color.BLACK;
        }

        @Nullable
        @Override
        public Color getForegroundFor(VirtualFile value) {
            return Color.GREEN;
        }

        @Override
        public Icon getIconFor(VirtualFile value) {
            if(TestSourcesFilter.isTestSources(value, project)){
                return AllIcons.Scope.Tests;
            } else {
                return value.getFileType().getIcon();
            }
        }



        private void openFile(VirtualFile file) {
            new OpenFileDescriptor(this.project, file).navigate(true);
        }

        private String getType(VirtualFile file){
            Matcher matcher = this.pattern.matcher(file.getCanonicalPath());
            if (matcher.find()){
                return matcher.group(1);
            } else {
                return "";
            }
        }
    }
}