package de.endrullis.idea.postfixtemplates.settings;

import com.intellij.ide.DataManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.*;
import com.intellij.util.Consumer;
import com.intellij.util.ObjectUtils;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import de.endrullis.idea.postfixtemplates.language.CptLang;
import de.endrullis.idea.postfixtemplates.languages.SupportedLanguages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

public class CptManagementTree extends CheckboxTree implements Disposable {
	@NotNull
	private final DefaultTreeModel model;
	@NotNull
	private final CheckedTreeNode  root;

	private final boolean canAddFile = true;

	CptManagementTree() {
		super(getRenderer(), new CheckedTreeNode(null));
		//canAddFile = ContainerUtil.find(providerToLanguage.keySet(), p -> StringUtil.isNotEmpty(p.getPresentableName())) != null;
		model = (DefaultTreeModel) getModel();
		root = (CheckedTreeNode) model.getRoot();

		TreeSelectionListener selectionListener = event -> selectionChanged();
		getSelectionModel().addTreeSelectionListener(selectionListener);
		Disposer.register(this, () -> getSelectionModel().removeTreeSelectionListener(selectionListener));
		DoubleClickListener doubleClickListener = new DoubleClickListener() {
			@Override
			protected boolean onDoubleClick(MouseEvent event) {
				TreePath location = getClosestPathForLocation(event.getX(), event.getY());
				return location != null && doubleClick(location.getLastPathComponent());
			}
		};
		doubleClickListener.installOn(this);
		Disposer.register(this, () -> doubleClickListener.uninstall(this));
		setRootVisible(false);
		setShowsRootHandles(true);
	}

	@Override
	protected void onDoubleClick(CheckedTreeNode node) {
		doubleClick(node);
	}

	private boolean doubleClick(@Nullable Object node) {
		if (node instanceof FileTreeNode && isEditable(((FileTreeNode) node).getFile())) {
			editFile((FileTreeNode) node);
			return true;
		}
		return false;
	}

	@Override
	public void dispose() {
		UIUtil.dispose(this);
	}

	@NotNull
	private static CheckboxTreeCellRenderer getRenderer() {
		return new CheckboxTreeCellRenderer() {
			@Override
			public void customizeRenderer(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
				if (!(value instanceof CheckedTreeNode)) return;
				CheckedTreeNode node = (CheckedTreeNode) value;

				final Color background = selected ? UIUtil.getTreeSelectionBackground() : UIUtil.getTreeTextBackground();
				FileTreeNode cptTreeNode = ObjectUtils.tryCast(node, FileTreeNode.class);
				SimpleTextAttributes attributes;
				if (cptTreeNode != null) {
					//Color fgColor = cptTreeNode.isChanged() || cptTreeNode.isNew() ? JBColor.BLUE : null;
					Color fgColor = cptTreeNode.getFile().isLocal() ? JBColor.BLUE : null;
					attributes = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, fgColor);
				} else {
					attributes = SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES;
				}
				getTextRenderer().append(StringUtil.notNullize(value.toString()),
					new SimpleTextAttributes(background, attributes.getFgColor(), JBColor.RED, attributes.getStyle()));

				if (cptTreeNode != null) {
					String url = cptTreeNode.getFile().getUrl().toString();
					getTextRenderer().append("  " + url, new SimpleTextAttributes(SimpleTextAttributes.STYLE_SMALLER, JBColor.GRAY), false);
				}
			}
		};
	}

	protected void selectionChanged() {

	}

	public void initTree(@NotNull Map<CptLang, List<CptVirtualFile>> lang2file) {
		root.removeAllChildren();
		
		Map<CptLang, List<FileTreeNode>> lang2nodes = new HashMap<>();
		for (Map.Entry<CptLang, List<CptVirtualFile>> entry : lang2file.entrySet()) {
			CptLang lang = entry.getKey();
			final List<FileTreeNode> nodes = lang2nodes.computeIfAbsent(lang, e -> new ArrayList<>());

			for (CptVirtualFile file : entry.getValue()) {
				nodes.add(new FileTreeNode(lang, file));
			}
		}

		for (Map.Entry<CptLang, List<FileTreeNode>> entry : lang2nodes.entrySet()) {
			DefaultMutableTreeNode langNode = findOrCreateLangNode(entry.getKey());
			for (FileTreeNode node : entry.getValue()) {
				langNode.add(new FileTreeNode(node.getLang(), node.getFile()));
			}
		}

		model.nodeStructureChanged(root);
		TreeUtil.expandAll(this);
	}

	@Nullable
	public CptVirtualFile getSelectedFile() {
		TreePath path = getSelectionModel().getSelectionPath();
		return getFileFromPath(path);
	}

	@Nullable
	private static CptVirtualFile getFileFromPath(@Nullable TreePath path) {
		if (path == null || !(path.getLastPathComponent() instanceof FileTreeNode)) {
			return null;
		}
		return ((FileTreeNode) path.getLastPathComponent()).getFile();
	}

	public void selectFile(@NotNull final CptVirtualFile file) {
		visitFileNodes(node -> {
			if (file.equals(node.getFile())) {
				TreeUtil.selectInTree(node, true, this, true);
			}
		});
	}

	private void visitFileNodes(@NotNull Consumer<FileTreeNode> consumer) {
		Enumeration languages = root.children();
		while (languages.hasMoreElements()) {
			CheckedTreeNode langNode = (CheckedTreeNode) languages.nextElement();
			Enumeration fileNodes = langNode.children();
			while (fileNodes.hasMoreElements()) {
				Object fileNode = fileNodes.nextElement();
				if (fileNode instanceof FileTreeNode) {
					consumer.consume((FileTreeNode) fileNode);
				}
			}
		}
	}

	public boolean canAddFile() {
		return canAddFile;
	}

	public void addFile(@NotNull AnActionButton button) {
		DefaultActionGroup group = new DefaultActionGroup() {{
			for (CptLang lang : SupportedLanguages.supportedLanguages) {
				add(new DumbAwareAction(lang.getNiceName()) {
					@Override
					public void actionPerformed(AnActionEvent anActionEvent) {
						
					}
				});
			}
		}};

		/*
		for (Map.Entry<CptLang, String> entry : myProviderToLanguage.entrySet()) {
			CptLang provider = entry.getKey();
			String providerName = provider.getPresentableName();
			if (StringUtil.isEmpty(providerName)) continue;
			group.add(new DumbAwareAction(providerName) {
				@Override
				public void actionPerformed(AnActionEvent e) {
					PostfixTemplateEditor editor = provider.createEditor(null);
					if (editor != null) {
						PostfixEditTemplateDialog dialog = new PostfixEditTemplateDialog(com.intellij.codeInsight.template.postfix.settings.PostfixTemplatesCheckboxTree.this, editor, providerName, null);
						if (dialog.showAndGet()) {
							String templateKey = dialog.getTemplateName();
							String templateId = PostfixTemplatesUtils.generateTemplateId(templateKey, provider);
							CptVirtualFile createdTemplate = editor.createTemplate(templateId, templateKey);

							CptTreeNode createdNode = new CptTreeNode(createdTemplate, provider, true);
							DefaultMutableTreeNode languageNode = findOrCreateLangNode(entry.getValue());
							languageNode.add(createdNode);
							myModel.nodeStructureChanged(languageNode);
							TreeUtil.selectNode(com.intellij.codeInsight.template.postfix.settings.PostfixTemplatesCheckboxTree.this, createdNode);
						}
					}
				}
			});
		}
		*/

		DataContext context = DataManager.getInstance().getDataContext(button.getContextComponent());
		ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(null, group, context,
			JBPopupFactory.ActionSelectionAid.ALPHA_NUMBERING, true, null);
		popup.show(ObjectUtils.assertNotNull(button.getPreferredPopupPoint()));
	}

	public boolean canEditSelectedFile() {
		TreePath[] selectionPaths = getSelectionModel().getSelectionPaths();
		return (selectionPaths == null || selectionPaths.length <= 1) && isEditable(getSelectedFile());
	}

	public void editSelectedFile() {
		TreePath path = getSelectionModel().getSelectionPath();
		Object lastPathComponent = path.getLastPathComponent();
		if (lastPathComponent instanceof FileTreeNode) {
			editFile((FileTreeNode) lastPathComponent);
		}
	}

	private void editFile(@NotNull FileTreeNode lastPathComponent) {
		CptVirtualFile file = lastPathComponent.getFile();
		CptLang lang = lastPathComponent.getLang();

		if (isEditable(file)) {
			CptVirtualFile fileToEdit = file;

			/*
			PostfixTemplateEditor editor = lang.createEditor(fileToEdit);
			if (editor == null) {
				editor = new DefaultPostfixTemplateEditor(lang, fileToEdit);
			}
			String providerName = StringUtil.notNullize(lang.getPresentableName());
			PostfixEditTemplateDialog dialog = new PostfixEditTemplateDialog(this, editor, providerName, fileToEdit);
			if (dialog.showAndGet()) {
				CptVirtualFile newTemplate = editor.createTemplate(file.getId(), dialog.getTemplateName());
				if (newTemplate.equals(file)) {
					return;
				}
				if (file.isBuiltin()) {
					CptVirtualFile builtin = file instanceof PostfixChangedBuiltinTemplate
						? ((PostfixChangedBuiltinTemplate) file).getBuiltinTemplate()
						: fileToEdit;
					lastPathComponent.setTemplate(new PostfixChangedBuiltinTemplate(newTemplate, builtin));
				} else {
					lastPathComponent.setTemplate(newTemplate);
				}
				myModel.nodeStructureChanged(lastPathComponent);
			}
			*/
		}
	}

	public boolean canRemoveSelectedFiles() {
		TreePath[] paths = getSelectionModel().getSelectionPaths();
		if (paths == null) {
			return false;
		}
		for (TreePath path : paths) {
			CptVirtualFile file = getFileFromPath(path);
			if (isEditable(file)) {
				return true;
			}
		}
		return false;
	}

	public void removeSelectedFiles() {
		TreePath[] paths = getSelectionModel().getSelectionPaths();
		if (paths == null) {
			return;
		}
		for (TreePath path : paths) {
			FileTreeNode lastPathComponent = ObjectUtils.tryCast(path.getLastPathComponent(),
				FileTreeNode.class);
			if (lastPathComponent == null) continue;
			// TODO: remove from FS
			CptVirtualFile file = lastPathComponent.getFile();
			TreeUtil.removeLastPathComponent(this, path);
		}
	}

	private static boolean isEditable(@Nullable CptVirtualFile file) {
		return file != null && file.isEditable();
	}

	@NotNull
	private DefaultMutableTreeNode findOrCreateLangNode(CptLang lang) {
		DefaultMutableTreeNode find = TreeUtil.findNode(root, n ->
			n instanceof LangTreeNode && lang.equals(((LangTreeNode) n).getLang()));

		if (find != null) {
			return find;
		}

		CheckedTreeNode languageNode = new LangTreeNode(lang);
		root.add(languageNode);

		return languageNode;
	}

	private static class LangTreeNode extends CheckedTreeNode {
		@NotNull
		private final CptLang lang;

		LangTreeNode(@NotNull CptLang lang) {
			super(lang.getNiceName());
			this.lang = lang;
		}

		@NotNull
		public CptLang getLang() {
			return lang;
		}
	}
}