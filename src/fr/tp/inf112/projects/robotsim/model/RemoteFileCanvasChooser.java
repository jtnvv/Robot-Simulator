package fr.tp.inf112.projects.robotsim.model;

import java.io.File;
import java.io.IOException;

import javax.swing.JOptionPane;

import fr.tp.inf112.projects.canvas.view.FileCanvasChooser;

public class RemoteFileCanvasChooser extends FileCanvasChooser {

	public RemoteFileCanvasChooser(final String fileExtension,
								   final String documentTypeLabel) {
		super(fileExtension, documentTypeLabel);
	}

	@Override
	protected String browseCanvases(final boolean open)
	throws IOException {
		if (open) {
			final String[] fileNames = (String[]) RemoteFactoryPersistenceManager.sendRequest(new File("."));

			return (String) JOptionPane.showInputDialog(getViewer(),
														"Select the model to open:",
														"Open",
														JOptionPane.QUESTION_MESSAGE,
														null,
														fileNames,
														fileNames.length > 0 ? fileNames[0] : null);
		}

		return JOptionPane.showInputDialog(getViewer(), "Enter the name of the model file:", "Save", JOptionPane.QUESTION_MESSAGE);
	}
}
