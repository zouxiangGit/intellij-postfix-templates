package de.endrullis.idea.postfixtemplates.utils;

import com.intellij.notification.*;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import de.endrullis.idea.postfixtemplates.language.CptUtil;
import de.endrullis.idea.postfixtemplates.languages.SupportedLanguages;
import de.endrullis.idea.postfixtemplates.settings.CptApplicationSettings;
import de.endrullis.idea.postfixtemplates.settings.CptPluginSettings;
import de.endrullis.idea.postfixtemplates.settings.CptVirtualFile;
import de.endrullis.idea.postfixtemplates.settings.WebTemplateFileLoader;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URL;
import java.util.Date;

import static de.endrullis.idea.postfixtemplates.language.CptUtil.downloadWebTemplatesInfoFile;
import static de.endrullis.idea.postfixtemplates.language.CptUtil.getWebTemplatesInfoFile;
import static de.endrullis.idea.postfixtemplates.language.CptUtil.moveOldTemplateFile;

/**
 * CPT update utilities.
 * 
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class CptUpdateUtils {

	private static final Object updateSync = new Object();

	public static void checkForWebTemplateUpdates(Project project) {
		final CptPluginSettings pluginSettings = CptApplicationSettings.getInstance().getPluginSettings();

		if (pluginSettings.isUpdateWebTemplatesAutomatically()) {

			NotificationGroup notificationGroup = new NotificationGroup("Custom Postfix Templates", NotificationDisplayType.STICKY_BALLOON, true);

			ProgressManager.getInstance().run(new Task.Backgroundable(project, "Updating Custom Postfix Web Templates") {
				public void run(@NotNull ProgressIndicator progressIndicator) {
					synchronized (updateSync) {
						for (String language : SupportedLanguages.supportedLanguageIds) {
							// eventually move old templates file to new directory
							moveOldTemplateFile(language);
						}

						boolean newTemplateFiles = false;

						// download the web templates file only once a day
						if (!getWebTemplatesInfoFile().exists() || new Date().getTime() - getWebTemplatesInfoFile().lastModified() > 1000 * 60 * 60 * 24) {
							try {
								downloadWebTemplatesInfoFile();

								progressIndicator.setFraction(0.10);
								//progressIndicator.setText("90% to finish");

								val webTemplateFiles = WebTemplateFileLoader.load(getWebTemplatesInfoFile());

								for (int i = 0; i < webTemplateFiles.length; i++) {
									val webTemplateFile = webTemplateFiles[i];

									CptVirtualFile cptFile = new CptVirtualFile(
										webTemplateFile.getId(),
										new URL(webTemplateFile.getUrl()),
										CptUtil.getTemplateFile(webTemplateFile.getLang(), webTemplateFile.getId()),
										true);

									newTemplateFiles |= CptUtil.downloadWebTemplateFile(cptFile);

									progressIndicator.setFraction(0.10 + 0.90 * (i + 1));
								}
							} catch (IOException e) {
								e.printStackTrace();
							}
						}

						progressIndicator.setFraction(1.0);
						progressIndicator.setText("finished");

						if (pluginSettings.getSettingsVersion() < 2) {
							Notification notification = notificationGroup.createNotification("Custom Postfix Templates 2.0", "Version 2.0 brings you user and web template files. Please check your <a href=\"settings\">settings</a> to configure the plugin.", NotificationType.INFORMATION,
								(notification1, hyperlinkEvent) -> {
									notification1.expire();
									CptUtil.openPluginSettings(project);
								}
							);

							Notifications.Bus.notify(notification, project);
						} else
						if (newTemplateFiles) {
							Notification notification = notificationGroup.createNotification("Custom Postfix Templates", "New web template files are available.  You can activate them in the <a href=\"settings\">settings</a>.", NotificationType.INFORMATION,
								(notification1, hyperlinkEvent) -> {
									notification1.expire();
									CptUtil.openPluginSettings(project);
								}
							);

							Notifications.Bus.notify(notification, project);
						}
					}
				}
			});

		}
	}

}
