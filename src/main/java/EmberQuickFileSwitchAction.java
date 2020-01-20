import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.TestSourcesFilter;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListSeparator;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.IconManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

        GlobalSearchScope scope = moduleWithDependenciesAndLibrariesScope(module, true);
        ArrayList<VirtualFile> files = new ArrayList<>();
        Collection<VirtualFile> jsFiles = getVirtualFilesByName(currentProject, currentFileNameWithoutExtension + ".js", scope);
        files.addAll(jsFiles.stream().filter(f -> !TestSourcesFilter.isTestSources(f, currentProject)).collect(Collectors.toList()));
        files.addAll(getVirtualFilesByName(currentProject, currentFileNameWithoutExtension + ".html", scope));
        files.addAll(getVirtualFilesByName(currentProject, currentFileNameWithoutExtension + ".hbs", scope));
        files.addAll(jsFiles.stream().filter(f -> TestSourcesFilter.isTestSources(f, currentProject)).collect(Collectors.toList()));

        JBPopup popup = JBPopupFactory.getInstance().createListPopup(new FileSelectionListPopupStep("Related Files", files, currentProject));
        popup.getContent().setBackground(Color.red);

        popup.showInFocusCenter();
    }



    private class FileSelectionListPopupStep extends BaseListPopupStep<VirtualFile> {

        public static final String ADAPTERS = "adapters";
        public static final String COMPONENTS = "components";
        public static final String CONTROLLERS = "controllers";
        public static final String HELPERS = "helpers";
        public static final String MODELS = "models";
        public static final String ROUTES = "routes";
        public static final String SERVICES = "services";
        public static final String TEMPLATES = "templates";
        public static final String TESTS_UNIT = "tests/unit";
        public static final String TESTS_ACCEPTANCE = "tests/acceptance";
        public static final String TESTS_INTEGRATION = "tests/integration";

        private List<String> fileTypes = Arrays.asList(
                ADAPTERS,
                COMPONENTS,
                CONTROLLERS,
                HELPERS,
                MODELS,
                ROUTES,
                SERVICES,
                TEMPLATES,
                TESTS_UNIT,
                TESTS_ACCEPTANCE,
                TESTS_INTEGRATION
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
            return Color.black;
        }


        @Nullable
        @Override
        public ListSeparator getSeparatorAbove(VirtualFile file) {
            if(TestSourcesFilter.isTestSources(file, project)){
                return new ListSeparator("test resources");
            }
            return null;
        }

        @Nullable
        @Override
        public Color getForegroundFor(VirtualFile value) {
            return Color.blue;
        }

        @Override
        public Icon getIconFor(VirtualFile file) {

            Icon testIcon = IconLoader.getIcon("/icons/service16.png");

            if(TestSourcesFilter.isTestSources(file, project)){
                return AllIcons.Scope.Tests;
            }


            return getIcon(file);


        }


        private Icon getIcon(VirtualFile file) {

            String type = this.getType(file);
            switch (type){
                case ADAPTERS:
                    return IconLoader.getIcon("/icons/adapter16.png");
                case COMPONENTS:
                    return IconLoader.getIcon("/icons/component16.png");
                case CONTROLLERS:
                    return IconLoader.getIcon("/icons/controller16.png");
                case MODELS:
                    return IconLoader.getIcon("/icons/model16.png");
                case ROUTES:
                    return IconLoader.getIcon("/icons/ROUTE_16.png");
                case SERVICES:
                    return IconLoader.getIcon("/icons/service16.png");
            }

            return file.getFileType().getIcon();



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