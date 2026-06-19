package ma.ensao.hotel.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Petit moteur d'export CSV générique.
 * <p>
 * Indépendant du domaine : il reçoit des en-têtes et une matrice de lignes,
 * échappe correctement les valeurs (guillemets, virgules, sauts de ligne) et
 * écrit le fichier en UTF-8 avec un BOM afin que les accents s'affichent
 * correctement dans Excel.
 *
 * @author ENSAO GI3
 */
public final class CSVExporter {

    private static final char SEPARATEUR = ';';   // ';' = standard Excel francophone

    private CSVExporter() {
    }

    /**
     * Écrit les données dans le fichier cible.
     *
     * @param fichier  fichier de destination (choisi via FileChooser)
     * @param entetes  noms des colonnes
     * @param lignes   liste de lignes, chaque ligne étant un tableau de cellules
     * @throws IOException en cas d'erreur d'écriture
     */
    public static void exporter(File fichier, String[] entetes, List<String[]> lignes) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(fichier), StandardCharsets.UTF_8))) {

            // BOM UTF-8 pour la compatibilité Excel
            writer.write('\uFEFF');

            writer.write(joindre(entetes));
            writer.newLine();

            for (String[] ligne : lignes) {
                writer.write(joindre(ligne));
                writer.newLine();
            }
        }
    }

    private static String joindre(String[] cellules) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cellules.length; i++) {
            if (i > 0) sb.append(SEPARATEUR);
            sb.append(echapper(cellules[i]));
        }
        return sb.toString();
    }

    /** Encadre la valeur de guillemets si elle contient un caractère spécial. */
    private static String echapper(String valeur) {
        if (valeur == null) return "";
        boolean doitEtreEntoure = valeur.contains(String.valueOf(SEPARATEUR))
                || valeur.contains("\"")
                || valeur.contains("\n")
                || valeur.contains("\r");
        String resultat = valeur.replace("\"", "\"\"");
        return doitEtreEntoure ? "\"" + resultat + "\"" : resultat;
    }
}
