package de.endrullis.idea.postfixtemplates.languages.scala;

import com.intellij.codeInsight.template.postfix.templates.StringBasedPostfixTemplate;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import de.endrullis.idea.postfixtemplates.templates.CustomPostfixTemplateProvider;
import org.jetbrains.annotations.NotNull;

public class ScalaPostfixTemplateProvider extends CustomPostfixTemplateProvider {

	@NotNull
	@Override
	protected String getLanguage() {
		return "scala";
	}

	@Override
	public String getPluginClassName() {
		return "org.jetbrains.plugins.scala.lang.completion.postfix.templates.ScalaStringBasedPostfixTemplate";
	}

	@NotNull
	@Override
	protected StringBasedPostfixTemplate createTemplate(String matchingClass, String conditionClass, String templateName, String description, String template) {
		return ScalaStringPostfixTemplateCreator.createTemplate(matchingClass, conditionClass, templateName, description, template);
	}

	@Override
	public void preExpand(@NotNull PsiFile file, @NotNull Editor editor) {
	}

	@Override
	public void afterExpand(@NotNull PsiFile file, @NotNull Editor editor) {
	}

	@NotNull
	@Override
	public PsiFile preCheck(@NotNull PsiFile copyFile, @NotNull Editor realEditor, int currentOffset) {
		return copyFile;
	}
}
