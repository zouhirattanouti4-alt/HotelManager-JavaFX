package ma.ensao.hotel.util;

import javafx.scene.Parent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/**
 * Gère la personnalisation de l'interface via le {@code ColorPicker}.
 * <p>
 * La couleur choisie est injectée comme <i>looked-up color</i> CSS
 * ({@code -app-accent}) sur le nœud racine de la scène. Toutes les règles CSS
 * qui référencent {@code -app-accent} sont alors mises à jour dynamiquement,
 * et la couleur se propage automatiquement à tous les enfants.
 *
 * @author ENSAO GI3
 */
public final class ThemeManager {

    /** Couleur d'accent par défaut de l'application (bleu nuit hôtelier). */
    public static final Color ACCENT_DEFAUT = Color.web("#1f6feb");

    /** Chemin de la feuille de style principale (classpath). */
    public static final String FEUILLE_STYLE = "/ma/ensao/hotel/css/styles.css";

    /** Palette de couleurs d'accent prédéfinies (libellé → code hex). */
    public static final String[][] PRESETS = {
            {"Bleu nuit",  "#1f6feb"},
            {"Émeraude",   "#0f9d58"},
            {"Or",         "#c79a3a"},
            {"Rubis",      "#c0392b"},
            {"Violet",     "#7c4dff"},
            {"Océan",      "#0aa5c2"}
    };

    private ThemeManager() {
    }

    /** Applique une couleur d'accent à toute l'interface. */
    public static void appliquerAccent(Parent racine, Color couleur) {
        if (racine == null || couleur == null) return;
        racine.setStyle("-app-accent: " + versHex(couleur) + ";");
    }

    /** Attache la feuille de style principale à un conteneur (ex : DialogPane). */
    public static void appliquerStyles(Region region) {
        if (region == null) return;
        var url = ThemeManager.class.getResource(FEUILLE_STYLE);
        if (url != null && !region.getStylesheets().contains(url.toExternalForm())) {
            region.getStylesheets().add(url.toExternalForm());
        }
    }

    /** Convertit une Color JavaFX en code hexadécimal CSS (#rrggbb). */
    public static String versHex(Color c) {
        return String.format("#%02X%02X%02X",
                (int) Math.round(c.getRed() * 255),
                (int) Math.round(c.getGreen() * 255),
                (int) Math.round(c.getBlue() * 255));
    }
}
