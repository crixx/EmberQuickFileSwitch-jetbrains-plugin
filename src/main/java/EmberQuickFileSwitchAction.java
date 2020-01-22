import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.TestSourcesFilter;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.PopupChooserBuilder;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
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
        if (module == null) return;

        String fileName = currentFile.getNameWithoutExtension();
        String normalizedFilename = normalizeFilename(fileName);

        GlobalSearchScope scope = moduleWithDependenciesAndLibrariesScope(module, true);


        // get all js files
        // Collection<VirtualFile> jsFiles = getAllFilesByExt(currentProject, "js", scope);

        // get JS files with name
        Collection<VirtualFile> jsFiles = getVirtualFilesByName(currentProject, normalizedFilename + ".js", scope);
        Collection<VirtualFile> hbsFiles = getVirtualFilesByName(currentProject, normalizedFilename + ".hbs", scope);
        Collection<VirtualFile> htmlFiles = getVirtualFilesByName(currentProject, normalizedFilename + ".html", scope);
        Collection<VirtualFile> scssFiles = getVirtualFilesByName(currentProject, normalizedFilename + ".scss", scope);
        scssFiles.addAll(getVirtualFilesByName(currentProject, "_" + normalizedFilename + ".scss", scope));
        Collection<VirtualFile> cssFiles = getVirtualFilesByName(currentProject, normalizedFilename + ".css", scope);

        // filter for source and test files
        Collection<VirtualFile> jsSrcFiles = jsFiles.stream().filter(f -> !TestSourcesFilter.isTestSources(f, currentProject)).collect(Collectors.toList());
        Collection<VirtualFile> jsTestFiles = jsFiles.stream().filter(f -> TestSourcesFilter.isTestSources(f, currentProject)).collect(Collectors.toList());
        Collection<VirtualFile> hbsSrcFiles = hbsFiles.stream().filter(f -> !TestSourcesFilter.isTestSources(f, currentProject)).collect(Collectors.toList());
        Collection<VirtualFile> hbsTestFiles = hbsFiles.stream().filter(f -> TestSourcesFilter.isTestSources(f, currentProject)).collect(Collectors.toList());
        Collection<VirtualFile> htmlSrcFiles = htmlFiles.stream().filter(f -> !TestSourcesFilter.isTestSources(f, currentProject)).collect(Collectors.toList());
        Collection<VirtualFile> htmlTestFiles = htmlFiles.stream().filter(f -> TestSourcesFilter.isTestSources(f, currentProject)).collect(Collectors.toList());
        Collection<VirtualFile> scssSrcFiles = scssFiles.stream().filter(f -> !TestSourcesFilter.isTestSources(f, currentProject)).collect(Collectors.toList());
        Collection<VirtualFile> scssTestFiles = scssFiles.stream().filter(f -> TestSourcesFilter.isTestSources(f, currentProject)).collect(Collectors.toList());
        Collection<VirtualFile> cssSrcFiles = cssFiles.stream().filter(f -> !TestSourcesFilter.isTestSources(f, currentProject)).collect(Collectors.toList());
        Collection<VirtualFile> cssTestFiles = cssFiles.stream().filter(f -> TestSourcesFilter.isTestSources(f, currentProject)).collect(Collectors.toList());

        ArrayList<VirtualFile> srcFiles = new ArrayList<>();

        srcFiles.addAll(jsSrcFiles);
        srcFiles.addAll(hbsSrcFiles);
        srcFiles.addAll(htmlSrcFiles);
        srcFiles.addAll(scssSrcFiles);
        srcFiles.addAll(cssSrcFiles);

        ArrayList<VirtualFile> testFiles = new ArrayList<>();
        testFiles.addAll(jsTestFiles);
        testFiles.addAll(hbsTestFiles);
        testFiles.addAll(htmlTestFiles);
        testFiles.addAll(scssTestFiles);
        testFiles.addAll(cssTestFiles);

        ArrayList<VirtualFile> files = new ArrayList<>();
        files.addAll(srcFiles);
        files.addAll(testFiles);

        final JBList<VirtualFile> list = new JBList(files);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        final ListCellRenderer renderer = new IconColoredListCellRenderer(currentProject);
        list.setCellRenderer(renderer);

        final PopupChooserBuilder builder = new PopupChooserBuilder(list);
        builder.setItemChoosenCallback(new Runnable() {
            @Override
            public void run() {
                new OpenFileDescriptor(currentProject, list.getSelectedValue()).navigate(true);
            }
        });

        final JBPopup popup = builder.createPopup();
        popup.showInFocusCenter();
    }

    private String normalizeFilename(String fileNameWithoutExtension) {
        String normalizedFilename = fileNameWithoutExtension.replace("-test", "");
        if (normalizedFilename.indexOf("_") == 0) {
            normalizedFilename = normalizedFilename.substring(1);
        }
        return normalizedFilename;
    }

    private class IconColoredListCellRenderer extends ColoredListCellRenderer<VirtualFile> {
       Project project;

        public IconColoredListCellRenderer(Project project) {
        this.project = project;
        }

        public static final String ADAPTERS = "adapters";
        public static final String SERIALIZER = "serializers";
        public static final String COMPONENTS = "components";
        public static final String CONTROLLERS = "controllers";
        public static final String HELPERS = "helpers";
        public static final String MODELS = "models";
        public static final String ROUTES = "routes";
        public static final String SERVICES = "services";
        public static final String TEMPLATES = "templates";

        public static final String TESTS_UNIT = "unit";
        public static final String TESTS_ACCEPTANCE = "acceptance";
        public static final String TESTS_INTEGRATION = "integration";

        private List<String> fileTypes = Arrays.asList(
                ADAPTERS,
                SERIALIZER,
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

        private Icon getIcon(VirtualFile file) {
            String type = this.getType(file);
            switch (type) {
                case ADAPTERS:
                    return IconLoader.getIcon("/icons/adapter16.png");
                case COMPONENTS:
                    return IconLoader.getIcon("/icons/component16.png");
                case CONTROLLERS:
                    return IconLoader.getIcon("/icons/controller16.png");
                case MODELS:
                    return IconLoader.getIcon("/icons/model16.png");
                case ROUTES:
                    return IconLoader.getIcon("/icons/route16.png");
                case SERVICES:
                    return IconLoader.getIcon("/icons/service16.png");
            }

            return file.getFileType().getIcon();
        }

        private String getType(VirtualFile file) {
            Matcher matcher = this.pattern.matcher(file.getCanonicalPath());
            if (matcher.find()) {
                return matcher.group(1);
            } else {
                return "";
            }
        }

        @Override
        protected void customizeCellRenderer(@NotNull JList<? extends VirtualFile> list, VirtualFile file, int index, boolean selected, boolean hasFocus) {
            setIcon(getIcon(file));
            setIconTextGap(20);
            appendTextPadding(150);
            append(file.getNameWithoutExtension());
            appendTextPadding(150);
            append(getType(file));
            appendTextPadding(300);
            append(file.getCanonicalPath());

            if (TestSourcesFilter.isTestSources(file, project)) {
                setBackground(JBColor.green);
            }
        }


    }
}