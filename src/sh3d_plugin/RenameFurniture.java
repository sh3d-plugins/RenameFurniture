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
		private String[] undoPi�ce;
		private String[] redoMeuble;
		private String[] redoPi�ce;

		// permet de g�rer les messages en fonction de la langue de l'utilisateur
		private final ResourceBundle resource = ResourceBundle.getBundle(
				"sh3d_plugin.ApplicationPlugin",
				Locale.getDefault(), getPluginClassLoader());

		/**
		 * Configures the plugin activation.
		 */
		public RenommerMeublesAction(String resourceBaseName, String actionPrefix, ClassLoader pluginClassLoader) {
			super(resourceBaseName, actionPrefix, pluginClassLoader, // utilise les propri�t�s de RENOMMER_MEUBLES
					!getHome().getRooms().isEmpty() && !getHome().getFurniture().isEmpty());
					// plugin actif si pi�ce et meuble d�j� pr�sents dans le logement

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
					long startTime = System.currentTimeMillis(); // d�clenchement du temps d'ex�cution
					long displayTime = 0;
					// restitue l'ancien nom du meuble ou groupe de meubles
					for(HomePieceOfFurniture meubles : getHome().getFurniture()){
						String ancienNomMeuble = undoMeuble[getHome().getFurniture().indexOf(meubles)];
						if(ancienNomMeuble != null){
							meubles.setName(ancienNomMeuble);
						}
						displayTime = affichageFenetre(startTime, fenetre);
					}
					// restitue l'ancien nom de la pi�ce
					for(Room pi�ces : getHome().getRooms()){
						String ancienNomPi�ce = undoPi�ce[getHome().getRooms().indexOf(pi�ces)];
						if(ancienNomPi�ce != null){
							String nomDePi�ce;
							if(ancienNomPi�ce.equals("...")){
								nomDePi�ce = "";
							} else {
								nomDePi�ce = ancienNomPi�ce;
							}
							pi�ces.setName(nomDePi�ce);
						}
						displayTime = affichageFenetre(startTime, fenetre);
					}
					temporisationAffichage(fenetre, displayTime);
					fenetre.setVisible(false);
				}

				@Override
				public void redo() throws CannotRedoException {
					super.redo();
					JFrame fenetre = new JFrame(resource.getString("r�tablissement"));
					parametreFenetre(fenetre);
					long startTime = System.currentTimeMillis(); // d�clenchement du temps d'ex�cution
					long displayTime = 0;
					// remet le nouveau nom du meuble ou groupe de meubles
					for(HomePieceOfFurniture meubles : getHome().getFurniture()){
						String nouveauNomMeuble = redoMeuble[getHome().getFurniture().indexOf(meubles)];
						if(nouveauNomMeuble != null){
							meubles.setName(nouveauNomMeuble);
						}
						displayTime = affichageFenetre(startTime, fenetre);
					}
					// remet le nouveau nom de la pi�ce
					for(Room pi�ces : getHome().getRooms()){
						String nouveauNomPi�ce = redoPi�ce[getHome().getRooms().indexOf(pi�ces)];
						if(nouveauNomPi�ce != null){
							pi�ces.setName(nouveauNomPi�ce);
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
			JFrame fenetre = new JFrame(resource.getString("ex�cution"));
			parametreFenetre(fenetre);
			long startTime = System.currentTimeMillis(); // d�clenchement du temps d'ex�cution
			long displayTime = 0;
			int nombreDeMeubles = getHome().getFurniture().size();
			int nombreDePi�ces = getHome().getRooms().size();
			boolean[] meublesDansPi�ce = new boolean [nombreDeMeubles];
			String[] nomPi�ceAvant = new String [nombreDePi�ces];
			String[] nomPi�ceApr�s = new String [nombreDePi�ces];
			String[] nomMeubleAvant = new String [nombreDeMeubles];
			boolean changement = false;
			String[] nomMeubleApr�s = new String [nombreDeMeubles];
			for(HomePieceOfFurniture meubles : getHome().getFurniture()){
				int indexMeuble = getHome().getFurniture().indexOf(meubles);
				float[][] coordonn�es = meubles.getPoints();
				String nomDuMeuble = meubles.getName();
				String ancienNomMeuble;
				for(Room pi�ces : getHome().getRooms()){
					int indexPi�ce = getHome().getRooms().indexOf(pi�ces);
					String nomDePi�ce = pi�ces.getName();
					// teste si le meuble ou groupe de meubles est centr� dans une pi�ce (plancher visible) du m�me niveau
					if(meubles.isAtLevel(pi�ces.getLevel()) &&
							pi�ces.isFloorVisible() && // �vite le probl�me de chevauchement avec des plafons seuls
							pi�ces.containsPoint(meubles.getX(), meubles.getY(), 10F/*marge 10cm*/) &&
							(pi�ces.containsPoint(coordonn�es[0][0], coordonn�es[0][1], 5F/*marge 5cm*/) || // �vite le probl�me
							pi�ces.containsPoint(coordonn�es[1][0], coordonn�es[1][1], 5F) || // des meubles centr�s
							pi�ces.containsPoint(coordonn�es[2][0], coordonn�es[2][1], 5F) || // avec tous les coins
							pi�ces.containsPoint(coordonn�es[3][0], coordonn�es[3][1], 5F))){ // ext�rieurs � la pi�ce
						meublesDansPi�ce[indexMeuble] = true;
						// teste si la pi�ce n'a pas de nom
						if(nomDePi�ce==null || nomDePi�ce.isEmpty()){
							// sauvegarde l'ancien nom de la pi�ce pour annulation (undo)
							nomPi�ceAvant[indexPi�ce] = "...";
							int num = indexPi�ce + 1;
							String num�ro = String.valueOf(num);
							if(num < 10){
								num�ro = 0 + num�ro;
							}
							nomDePi�ce = resource.getString("pi�ceSansNom") + num�ro;
							pi�ces.setName(nomDePi�ce);
							// sauvegarde le nouveau nom de la pi�ce pour r�tablissement (redo)
							nomPi�ceApr�s[indexPi�ce] = nomDePi�ce;
						}
						// teste si le meuble ou groupe de meubles a �t� renomm�
						if(nomDuMeuble.startsWith("<")){
							int posChar0 = nomDuMeuble.indexOf('<');
							int posChar1 = nomDuMeuble.indexOf('>');
							String pi�ceMeubleRenomm� = nomDuMeuble.substring(posChar0+1, posChar1);
							ancienNomMeuble = nomDuMeuble.substring(posChar1+1);
							// teste si la pi�ce a un nom diff�rent de celui du meuble renomm�
							if(!(nomDePi�ce.equals(pi�ceMeubleRenomm�))){
								// sauvegarde l'ancien nom du meuble pour annulation (undo)
								nomMeubleAvant[indexMeuble] = nomDuMeuble;
								// remplace le nom de la pi�ce du meuble renomm�
								nomDuMeuble = "<" + nomDePi�ce + ">" + ancienNomMeuble;
								meubles.setName(nomDuMeuble);
								changement = true;
								// sauvegarde le nouveau nom du meuble pour r�tablissement (redo)
								nomMeubleApr�s[indexMeuble] = nomDuMeuble;
								// s�lectionne le meuble renomm�
								getHome().deselectItem(meubles);
								getHomeController().getPlanController().toggleItemSelection(meubles);
							}
						} else {
							// sauvegarde l'ancien nom du meuble pour annulation (undo)
							nomMeubleAvant[indexMeuble] = nomDuMeuble;
							// ajoute le nom de la pi�ce au meuble
							nomDuMeuble = "<" + nomDePi�ce + ">" + nomDuMeuble;
							meubles.setName(nomDuMeuble);
							changement = true;
							// sauvegarde le nouveau nom du meuble pour r�tablissement (redo)
							nomMeubleApr�s[indexMeuble] = nomDuMeuble;
							// s�lectionne le meuble renomm�
							getHome().deselectItem(meubles);
							getHomeController().getPlanController().toggleItemSelection(meubles);
						}
						break; // arr�te la boucle recherche de pi�ces
					}
				}
				if(meublesDansPi�ce[indexMeuble]==false){
					// teste si le meuble ou groupe de meubles qui n'est pas dans une pi�ce a �t� renomm�
					if(nomDuMeuble.startsWith("<")){
						// sauvegarde l'ancien nom du meuble pour annulation (undo)
						nomMeubleAvant[indexMeuble] = nomDuMeuble;
						int posChar1 = nomDuMeuble.indexOf('>');
						ancienNomMeuble = nomDuMeuble.substring(posChar1+1);
						// supprime le nom de la pi�ce du meuble ou groupe de meubles renomm�
						nomDuMeuble = ancienNomMeuble;
						meubles.setName(nomDuMeuble);
						changement = true;
						// sauvegarde le nouveau nom du meuble pour r�tablissement (redo)
						nomMeubleApr�s[indexMeuble] = nomDuMeuble;
						// s�lectionne le meuble renomm�
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
				redoMeuble = nomMeubleApr�s;
				undoPi�ce = nomPi�ceAvant;
				redoPi�ce = nomPi�ceApr�s;
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
			window.setSize(268, 118); // largeur et hauteur de la fen�tre
			window.setResizable(false);
			window.setLocationRelativeTo(null); // fen�tre centr�e
		}

		/**
		 * Displays the window after 1s.
		 * 
		 * @param startTime Execution time.
		 * @param fenetre The window.
		 * @return displayTime Beginning of the display time.
		 */
		private long affichageFenetre(long startTime, JFrame fenetre) {
			// affichage de la fen�tre si temps d'ex�cution >= 1s
			long estimatedTime = System.currentTimeMillis() - startTime;
			long displayTime = 0;
			if(estimatedTime >= 1000 && !fenetre.isVisible()){
				fenetre.setVisible(true);
				displayTime = System.currentTimeMillis(); // d�clenchement du temps d'affichage
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