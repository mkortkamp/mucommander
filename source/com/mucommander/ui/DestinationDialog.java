
package com.mucommander.ui;

import com.mucommander.file.AbstractFile;
import com.mucommander.file.util.FileSet;
import com.mucommander.file.util.FileToolkit;
import com.mucommander.text.Translator;
import com.mucommander.ui.comp.dialog.DialogToolkit;
import com.mucommander.ui.comp.dialog.FocusDialog;
import com.mucommander.ui.comp.dialog.YBoxPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 * Abstract Dialog which displays an input field in order to enter a destination path.
 * This dialog is used by CopyDialog, MoveDialog, UnpackDialog and DownloadDialog.
 *
 * @author Maxence Bernard
 */
public abstract class DestinationDialog extends FocusDialog implements ActionListener {

    protected MainFrame mainFrame;
    protected FileSet files;
	
    protected JTextField pathField;
    protected JComboBox fileExistsActionComboBox;
    protected JButton okButton;
    protected JButton cancelButton;

    protected String errorDialogTitle = Translator.get("move_dialog.error_title");
	
    // Dialog size constraints
    protected final static Dimension MINIMUM_DIALOG_DIMENSION = new Dimension(320,0);	
    // Dialog width should not exceed 360, height is not an issue (always the same)
    protected final static Dimension MAXIMUM_DIALOG_DIMENSION = new Dimension(400,10000);	

	
    private final static int DEFAULT_ACTIONS[] = {
        FileCollisionDialog.CANCEL_ACTION,
        FileCollisionDialog.SKIP_ACTION,
        FileCollisionDialog.OVERWRITE_ACTION,
        FileCollisionDialog.OVERWRITE_IF_OLDER_ACTION,
        FileCollisionDialog.RESUME_ACTION
    };

    private final static String DEFAULT_ACTIONS_TEXT[] = {
        FileCollisionDialog.CANCEL_TEXT,
        FileCollisionDialog.SKIP_TEXT,
        FileCollisionDialog.OVERWRITE_TEXT,
        FileCollisionDialog.OVERWRITE_IF_OLDER_TEXT,
        FileCollisionDialog.RESUME_TEXT
    };
	

    /**
     * Creates a new DestinationDialog.
     *
     * @param mainFrame the main frame this dialog is attached to.
     */
    public DestinationDialog(MainFrame mainFrame, FileSet files) {
        super(mainFrame, null, mainFrame);
        this.mainFrame = mainFrame;
        this.files = files;
    }
	
	
    /**
     * Creates a new DestinationDialog.
     *
     * @param mainFrame the main frame this dialog is attached to.
     */
    public DestinationDialog(MainFrame mainFrame, FileSet files, String title, String labelText, String okText, String errorDialogTitle) {
        this(mainFrame, files);
		
        init(title, labelText, okText, errorDialogTitle);
    }
	
	
    protected void init(String title, String labelText, String okText, String errorDialogTitle) {
        this.errorDialogTitle = errorDialogTitle;

        setTitle(title);
		
        Container contentPane = getContentPane();
        YBoxPanel mainPanel = new YBoxPanel();
		
        JLabel label = new JLabel(labelText+" :");
        mainPanel.add(label);

        // Create path textfield
        pathField = new JTextField();
        pathField.addActionListener(this);
        mainPanel.add(pathField);
        mainPanel.addSpace(10);

        // Path field will receive initial focus
        setInitialFocusComponent(pathField);		
		
        // OK / Cancel buttons panel
        okButton = new JButton(okText);
        cancelButton = new JButton(Translator.get("cancel"));
        contentPane.add(DialogToolkit.createOKCancelPanel(okButton, cancelButton, this), BorderLayout.SOUTH);

        // Selects OK when enter is pressed
        getRootPane().setDefaultButton(okButton);

        // Checkbox that allows the user to choose the default action when a file already exists in destination
        mainPanel.add(new JLabel(Translator.get("destination_dialog.file_exists_action")));
        fileExistsActionComboBox = new JComboBox();
        fileExistsActionComboBox.addItem(Translator.get("ask"));
        int nbChoices = DEFAULT_ACTIONS_TEXT.length;
        for(int i=0; i<nbChoices; i++)
            fileExistsActionComboBox.addItem(DEFAULT_ACTIONS_TEXT[i]);
        mainPanel.add(fileExistsActionComboBox);
        mainPanel.addSpace(10);
		
        contentPane.add(mainPanel, BorderLayout.NORTH);
		
        // Set minimum/maximum dimension
        setMinimumSize(MINIMUM_DIALOG_DIMENSION);
        setMaximumSize(MAXIMUM_DIALOG_DIMENSION);
    }


    protected void setTextField(String text) {
        pathField.setText(text);
        // Text is selected so that user can directly type and replace path
        pathField.setSelectionStart(0);
        pathField.setSelectionEnd(text.length());
    }


    protected void setTextField(String text, int selStart, int selEnd) {
        pathField.setText(text);
        // Text is selected so that user can directly type and replace path
        pathField.setSelectionStart(selStart);
        pathField.setSelectionEnd(selEnd);
    }
	
	
    /**
     * Displays an error message.
     */
    protected void showErrorDialog(String msg) {
        JOptionPane.showMessageDialog(mainFrame, msg, errorDialogTitle, JOptionPane.ERROR_MESSAGE);
    }


    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        dispose();
		
        // OK action
        if(source == okButton || source == pathField) {
            okPressed();
        }
    }

	
    /**
     * This method is invoked when the OK button is pressed.
     */
    private void okPressed() {
        String destPath = pathField.getText();

        // Resolves destination folder
        // TODO: this should be done in the job's thread because AbstractFile creation can lock the main thread if the file is on a remote filesystem
        Object ret[] = FileToolkit.resolvePath(destPath, mainFrame.getActiveTable().getCurrentFolder());
        // The path entered doesn't correspond to any existing folder
        if (ret==null || (files.size()>1 && ret[1]!=null)) {
            showErrorDialog(Translator.get("this_folder_does_not_exist", destPath));
            return;
        }

        AbstractFile destFolder = (AbstractFile)ret[0];
        String newName = (String)ret[1];
		
        // Retrieve default action when a file exists in destination, default choice
        // (if not specified by the user) is 'Ask'
        int defaultFileExistsAction = fileExistsActionComboBox.getSelectedIndex();
        if(defaultFileExistsAction==0)
            defaultFileExistsAction = FileCollisionDialog.ASK_ACTION;
        else
            defaultFileExistsAction = DEFAULT_ACTIONS[defaultFileExistsAction-1];
        // We don't remember default action on purpose: we want the user to specify it each time,
        // it would be too dangerous otherwise.
		
        startJob(destFolder, newName, defaultFileExistsAction);
    }
	
	
    protected abstract void startJob(AbstractFile destFolder, String newName, int defaultFileExistsAction);
	
}
