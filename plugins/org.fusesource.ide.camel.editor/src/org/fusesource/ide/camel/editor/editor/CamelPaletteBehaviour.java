package org.fusesource.ide.camel.editor.editor;

import org.eclipse.gef.dnd.TemplateTransferDragSourceListener;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.ui.palette.PaletteViewer;
import org.eclipse.gef.ui.palette.PaletteViewerProvider;
import org.eclipse.graphiti.ui.editor.DefaultPaletteBehavior;
import org.eclipse.graphiti.ui.editor.DiagramEditor;

/**
 * @author lhein
 */
public class CamelPaletteBehaviour extends DefaultPaletteBehavior {
	
	/**
	 * @param diagramEditor
	 */
	public CamelPaletteBehaviour(DiagramEditor diagramEditor) {
		super(diagramEditor);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.ui.editor.DefaultPaletteBehavior#createPaletteRoot()
	 */
	@Override
	protected PaletteRoot createPaletteRoot() {
		return new CamelPaletteRoot(((RiderDesignEditor)diagramEditor).getConfigurationProvider());
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.ui.editor.DefaultPaletteBehavior#createPaletteViewerProvider()
	 */
	@Override
	protected PaletteViewerProvider createPaletteViewerProvider() {
		return new PaletteViewerProvider(diagramEditor.getEditDomain()) {
			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.gef.ui.palette.PaletteViewerProvider#
			 * configurePaletteViewer(org.eclipse.gef.ui.palette.PaletteViewer)
			 */
			@Override
			protected void configurePaletteViewer(PaletteViewer viewer) {
				super.configurePaletteViewer(viewer);
				// create a drag source listener for this palette viewer
				// together with an appropriate transfer drop target listener,
				// this will enable
				// model element creation by dragging a
				// CombinatedTemplateCreationEntries
				// from the palette into the editor
				// @see ShapesEditor#createTransferDropTargetListener()
				viewer.addDragSourceListener(new TemplateTransferDragSourceListener(
						viewer));
			}
		};
	}
}