package sh3d_plugin;

import javax.swing.JFrame;

import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.JOptionPane;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import com.eteks.sweethome3d.model.CollectionEvent;
import com.eteks.sweethome3d.model.CollectionListener;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.Room;
import com.eteks.sweethome3d.plugin.Plugin;
import com.eteks.sweethome3d.plugin.PluginAction;

/**
 * A plugin that adds the name of rooms to the furniture.
 * 
 * @author Enko Nyito
 */
public class RenameFurniture extends Plugin {

	@Override
	public PluginAction[] getActions() {
		return new PluginAction [] {
				new RenommerMeublesAction(
						"sh3d_plugin.ApplicationPlugin", // MyResources.properties
						"RENOMMER_MEUBLES", getPluginClassLoader())
				};
	}

	public class RenommerMeublesAction extends PluginAction {

		private String[] undoMeuble;
		private String[] undoPièce;
		private String[] redoMeuble;
		private String[] redoPièce;

		// permet de gérer les messages en fonction de la langue de l'utilisateur
		private final ResourceBundle resource = ResourceBundle.getBundle(
				"sh3d_plugin.ApplicationPlugin",
				Locale.getDefault(), getPluginClassLoader());

		/**
		 * Configures the plugin activation.
		 */
		public RenommerMeublesAction(String resourceBaseName, String actionPrefix, ClassLoader pluginClassLoader) {
			super(resourceBaseName, actionPrefix, pluginClassLoader, // utilise les propriétés de RENOMMER_MEUBLES
					!getHome().getRooms().isEmpty() && !getHome().getFurniture().isEmpty());
					// plugin actif si pièce et meuble déjà présents dans le logement

			// Add a listener to enable action only when collection of room and furniture isn't empty
			getHome().addRoomsListener(new CollectionListener<Room>(){
				public void collectionChanged(CollectionEvent<Room> ev) {
					setEnabled(!getHome().getRooms().isEmpty() && !getHome().getFurniture().isEmpty());
				}
			});
			getHome().addFurnitureListener(new CollectionListener<HomePieceOfFurniture>(){
				public void collectionChanged(CollectionEvent<HomePieceOfFurniture> ev) {
					setEnabled(!getHome().getRooms().isEmpty() && !getHome().getFurniture().isEmpty());
				}
			});
		}		

		/**
		 * Renames the furniture and registers undo|redo actions.
		 */
		@Override
		public void execute() {
			renameFurniture();

			getUndoableEditSupport().postEdit(new AbstractUndoableEdit() {
				private static final long serialVersionUID = 1L;

				@Override
				public void undo() throws CannotUndoException {
					super.undo();
					JFrame fenetre = new JFrame(resource.getString("annulation"));
					parametreFenetre(fenetre);
					long startTime = System.currentTimeMillis(); // déclenchement du temps d'exécution
					long displayTime = 0;
					// restitue l'ancien nom du meuble ou groupe de meubles
					for(HomePieceOfFurniture meubles : getHome().getFurniture()){
						String ancienNomMeuble = undoMeuble[getHome().getFurniture().indexOf(meubles)];
						if(ancienNomMeuble != null){
							meubles.setName(ancienNomMeuble);
						}
						displayTime = affichageFenetre(startTime, fenetre);
					}
					// restitue l'ancien nom de la pièce
					for(Room pièces : getHome().getRooms()){
						String ancienNomPièce = undoPièce[getHome().getRooms().indexOf(pièces)];
						if(ancienNomPièce != null){
							String nomDePièce;
							if(ancienNomPièce.equals("...")){
								nomDePièce = "";
							} else {
								nomDePièce = ancienNomPièce;
							}
							pièces.setName(nomDePièce);
						}
						displayTime = affichageFenetre(startTime, fenetre);
					}
					temporisationAffichage(fenetre, displayTime);
					fenetre.setVisible(false);
				}

				@Override
				public void redo() throws CannotRedoException {
					super.redo();
					JFrame fenetre = new JFrame(resource.getString("rétablissement"));
					parametreFenetre(fenetre);
					long startTime = System.currentTimeMillis(); // déclenchement du temps d'exécution
					long displayTime = 0;
					// remet le nouveau nom du meuble ou groupe de meubles
					for(HomePieceOfFurniture meubles : getHome().getFurniture()){
						String nouveauNomMeuble = redoMeuble[getHome().getFurniture().indexOf(meubles)];
						if(nouveauNomMeuble != null){
							meubles.setName(nouveauNomMeuble);
						}
						displayTime = affichageFenetre(startTime, fenetre);
					}
					// remet le nouveau nom de la pièce
					for(Room pièces : getHome().getRooms()){
						String nouveauNomPièce = redoPièce[getHome().getRooms().indexOf(pièces)];
						if(nouveauNomPièce != null){
							pièces.setName(nouveauNomPièce);
						}
						displayTime = affichageFenetre(startTime, fenetre);
					}
					temporisationAffichage(fenetre, displayTime);
					fenetre.setVisible(false);
				}

				@Override
				public String getPresentationName() {
					return getPropertyValue(Property.NAME).toString();
				}
			});
		}

		/**
		 * Renames the furniture by rooms and sorts them by name.
		 */
		private void renameFurniture() {
			JFrame fenetre = new JFrame(resource.getString("exécution"));
			parametreFenetre(fenetre);
			long startTime = System.currentTimeMillis(); // déclenchement du temps d'exécution
			long displayTime = 0;
			int nombreDeMeubles = getHome().getFurniture().size();
			int nombreDePièces = getHome().getRooms().size();
			boolean[] meublesDansPièce = new boolean [nombreDeMeubles];
			String[] nomPièceAvant = new String [nombreDePièces];
			String[] nomPièceAprès = new String [nombreDePièces];
			String[] nomMeubleAvant = new String [nombreDeMeubles];
			boolean changement = false;
			String[] nomMeubleAprès = new String [nombreDeMeubles];
			for(HomePieceOfFurniture meubles : getHome().getFurniture()){
				int indexMeuble = getHome().getFurniture().indexOf(meubles);
				float[][] coordonnées = meubles.getPoints();
				String nomDuMeuble = meubles.getName();
				String ancienNomMeuble;
				for(Room pièces : getHome().getRooms()){
					int indexPièce = getHome().getRooms().indexOf(pièces);
					String nomDePièce = pièces.getName();
					// teste si le meuble ou groupe de meubles est centré dans une pièce (plancher visible) du même niveau
					if(meubles.isAtLevel(pièces.getLevel()) &&
							pièces.isFloorVisible() && // évite le problème de chevauchement avec des plafons seuls
							pièces.containsPoint(meubles.getX(), meubles.getY(), 10F/*marge 10cm*/) &&
							(pièces.containsPoint(coordonnées[0][0], coordonnées[0][1], 5F/*marge 5cm*/) || // évite le problème
							pièces.containsPoint(coordonnées[1][0], coordonnées[1][1], 5F) || // des meubles centrés
							pièces.containsPoint(coordonnées[2][0], coordonnées[2][1], 5F) || // avec tous les coins
							pièces.containsPoint(coordonnées[3][0], coordonnées[3][1], 5F))){ // extérieurs à la pièce
						meublesDansPièce[indexMeuble] = true;
						// teste si la pièce n'a pas de nom
						if(nomDePièce==null || nomDePièce.isEmpty()){
							// sauvegarde l'ancien nom de la pièce pour annulation (undo)
							nomPièceAvant[indexPièce] = "...";
							int num = indexPièce + 1;
							String numéro = String.valueOf(num);
							if(num < 10){
								numéro = 0 + numéro;
							}
							nomDePièce = resource.getString("pièceSansNom") + numéro;
							pièces.setName(nomDePièce);
							// sauvegarde le nouveau nom de la pièce pour rétablissement (redo)
							nomPièceAprès[indexPièce] = nomDePièce;
						}
						// teste si le meuble ou groupe de meubles a été renommé
						if(nomDuMeuble.startsWith("<")){
							int posChar0 = nomDuMeuble.indexOf('<');
							int posChar1 = nomDuMeuble.indexOf('>');
							String pièceMeubleRenommé = nomDuMeuble.substring(posChar0+1, posChar1);
							ancienNomMeuble = nomDuMeuble.substring(posChar1+1);
							// teste si la pièce a un nom différent de celui du meuble renommé
							if(!(nomDePièce.equals(pièceMeubleRenommé))){
								// sauvegarde l'ancien nom du meuble pour annulation (undo)
								nomMeubleAvant[indexMeuble] = nomDuMeuble;
								// remplace le nom de la pièce du meuble renommé
								nomDuMeuble = "<" + nomDePièce + ">" + ancienNomMeuble;
								meubles.setName(nomDuMeuble);
								changement = true;
								// sauvegarde le nouveau nom du meuble pour rétablissement (redo)
								nomMeubleAprès[indexMeuble] = nomDuMeuble;
								// sélectionne le meuble renommé
								getHome().deselectItem(meubles);
								getHomeController().getPlanController().toggleItemSelection(meubles);
							}
						} else {
							// sauvegarde l'ancien nom du meuble pour annulation (undo)
							nomMeubleAvant[indexMeuble] = nomDuMeuble;
							// ajoute le nom de la pièce au meuble
							nomDuMeuble = "<" + nomDePièce + ">" + nomDuMeuble;
							meubles.setName(nomDuMeuble);
							changement = true;
							// sauvegarde le nouveau nom du meuble pour rétablissement (redo)
							nomMeubleAprès[indexMeuble] = nomDuMeuble;
							// sélectionne le meuble renommé
							getHome().deselectItem(meubles);
							getHomeController().getPlanController().toggleItemSelection(meubles);
						}
						break; // arrête la boucle recherche de pièces
					}
				}
				if(meublesDansPièce[indexMeuble]==false){
					// teste si le meuble ou groupe de meubles qui n'est pas dans une pièce a été renommé
					if(nomDuMeuble.startsWith("<")){
						// sauvegarde l'ancien nom du meuble pour annulation (undo)
						nomMeubleAvant[indexMeuble] = nomDuMeuble;
						int posChar1 = nomDuMeuble.indexOf('>');
						ancienNomMeuble = nomDuMeuble.substring(posChar1+1);
						// supprime le nom de la pièce du meuble ou groupe de meubles renommé
						nomDuMeuble = ancienNomMeuble;
						meubles.setName(nomDuMeuble);
						changement = true;
						// sauvegarde le nouveau nom du meuble pour rétablissement (redo)
						nomMeubleAprès[indexMeuble] = nomDuMeuble;
						// sélectionne le meuble renommé
						getHome().deselectItem(meubles);
						getHomeController().getPlanController().toggleItemSelection(meubles);
					}
				}
				displayTime = affichageFenetre(startTime, fenetre);
			}
			if(changement){
				temporisationAffichage(fenetre, displayTime);
				fenetre.setVisible(false);
				undoMeuble = nomMeubleAvant;
				redoMeuble = nomMeubleAprès;
				undoPièce = nomPièceAvant;
				redoPièce = nomPièceAprès;
				// tri les meubles par nom dans l'ordre ascendant
				if(getHome().getFurnitureSortedProperty() != HomePieceOfFurniture.SortableProperty.NAME){
				getHome().setFurnitureSortedProperty(HomePieceOfFurniture.SortableProperty.NAME);
				}
			} else {
				String message = resource.getString("sansChangement");
				JOptionPane.showMessageDialog(null, message);
			}
		}

		/**
		 * Configures a window.
		 * 
		 * @param window A simple window.
		 */
		private void parametreFenetre(JFrame window) {
			window.setSize(268, 118); // largeur et hauteur de la fenêtre
			window.setResizable(false);
			window.setLocationRelativeTo(null); // fenêtre centrée
		}

		/**
		 * Displays the window after 1s.
		 * 
		 * @param startTime Execution time.
		 * @param fenetre The window.
		 * @return displayTime Beginning of the display time.
		 */
		private long affichageFenetre(long startTime, JFrame fenetre) {
			// affichage de la fenêtre si temps d'exécution >= 1s
			long estimatedTime = System.currentTimeMillis() - startTime;
			long displayTime = 0;
			if(estimatedTime >= 1000 && !fenetre.isVisible()){
				fenetre.setVisible(true);
				displayTime = System.currentTimeMillis(); // déclenchement du temps d'affichage
			}
			return displayTime;
		}

		/**
		 * Adjusts the display time to minimum 1s.
		 * 
		 * @param fenetre The displayed window.
		 * @param displayTime Beginning of the display time.
		 */
		private void temporisationAffichage(JFrame fenetre, long displayTime) {
			// temporisation si temps d'affichage < 1s
			if(fenetre.isVisible()){
				long estimatedTime = System.currentTimeMillis() - displayTime; // temps d'affichage
				if(estimatedTime < 1000){
					long remainingTime = 1000 - estimatedTime;
					long startTime = System.currentTimeMillis();
					do{
						estimatedTime = System.currentTimeMillis() - startTime;
					} while(estimatedTime<remainingTime);
				}
			}
		}
	}
}