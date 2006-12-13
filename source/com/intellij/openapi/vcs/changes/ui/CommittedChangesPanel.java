/*
 * Copyright (c) 2000-2006 JetBrains s.r.o. All Rights Reserved.
 */

/*
 * Created by IntelliJ IDEA.
 * User: yole
 * Date: 05.12.2006
 * Time: 19:39:22
 */
package com.intellij.openapi.vcs.changes.ui;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.CommittedChangesProvider;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList;
import com.intellij.openapi.vcs.versionBrowser.ChangeBrowserSettings;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.diagnostic.Logger;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CommittedChangesPanel extends JPanel {
  private static final Logger LOG = Logger.getInstance("#com.intellij.openapi.vcs.changes.ui.CommittedChangesPanel");

  private CommittedChangesBrowser myBrowser;
  private final Project myProject;
  private CommittedChangesProvider myProvider;
  private ChangeBrowserSettings mySettings;
  private VirtualFile myRoot;

  public CommittedChangesPanel(Project project, final CommittedChangesProvider provider, final ChangeBrowserSettings settings) {
    super(new BorderLayout());
    mySettings = settings;
    myProject = project;
    myProvider = provider;
    myBrowser = new CommittedChangesBrowser(project, new CommittedChangesTableModel(new ArrayList<CommittedChangeList>()));
    add(myBrowser, BorderLayout.CENTER);

    DefaultActionGroup toolbarActionGroup = new DefaultActionGroup();
    toolbarActionGroup.add(new RefreshAction());
    toolbarActionGroup.add(new FilterAction());
    ActionToolbar toolBar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, toolbarActionGroup, true);
    myBrowser.addToolBar(toolBar.getComponent());
    //myComponent.add(toolBar.getComponent(), BorderLayout.WEST);
  }

  public void setRoot(final VirtualFile root) {
    myRoot = root;
  }

  public void refreshChanges() {
    final Ref<VcsException> refEx = new Ref<VcsException>();
    ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
      public void run() {
        try {
          final List<CommittedChangeList> list;
          if (myRoot == null) {
            list = myProvider.getAllCommittedChanges(mySettings, 50);
          }
          else {
            list = myProvider.getCommittedChanges(mySettings, myRoot);
          }
          myBrowser.setModel(new CommittedChangesTableModel(list, myProvider.getColumns()));
        }
        catch (VcsException ex) {
          refEx.set(ex);
        }
      }
    }, "Loading changes", true, myProject);
    if (!refEx.isNull()) {
      LOG.error(refEx.get());
      Messages.showErrorDialog(myProject, "Error refreshing view: " + StringUtil.join(refEx.get().getMessages(), "\n"), "Committed Changes");
    }
  }

  private void setChangesFilter() {
    CommittedChangesFilterDialog filterDialog = new CommittedChangesFilterDialog(myProject, myProvider.createFilterUI(), mySettings);
    filterDialog.show();
    if (filterDialog.isOK()) {
      mySettings = filterDialog.getSettings();
      refreshChanges();
    }
  }

  private class RefreshAction extends AnAction {
    public RefreshAction() {
      super("Refresh", "Refresh the list of committed changes", IconLoader.getIcon("/vcs/refresh.png"));
    }

    public void actionPerformed(AnActionEvent e) {
      refreshChanges();
    }
  }

  private class FilterAction extends AnAction {
    public FilterAction() {
      super("Filter", "Change filtering criteria", IconLoader.getIcon("/ant/filter.png"));
    }

    public void actionPerformed(AnActionEvent e) {
      setChangesFilter();
    }
  }
}