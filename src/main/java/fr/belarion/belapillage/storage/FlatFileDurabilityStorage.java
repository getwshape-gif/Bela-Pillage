package fr.belarion.belapillage.storage;

import fr.belarion.belapillage.durability.BlockKey;
import fr.belarion.belapillage.durability.DurabilityManager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

/**
 * Persistance sous forme d'un fichier binaire compact (pas de YAML) : un simple flux de
 * (monde UTF, x int, y int, z int, coups encaisses int) par entree endommagee.
 * <p>
 * Choix delibere plutot qu'un stockage YAML/Bukkit Configuration : YAML devient tres
 * lent a parser/serialiser avec plusieurs milliers d'entrees imbriquees, alors qu'un
 * flux binaire lineaire de cette taille se charge et se sauvegarde quasi instantanement
 * (quelques millisecondes, meme pour plusieurs dizaines de milliers de blocs), sans
 * dependance externe (pas de driver JDBC a embarquer/charger).
 * <p>
 * Ecriture atomique : on ecrit d'abord dans un fichier temporaire, puis on le renomme a
 * la place du fichier final une fois l'ecriture terminee avec succes, pour ne jamais
 * laisser un fichier de donnees corrompu/tronque en cas de crash pendant la sauvegarde.
 */
public final class FlatFileDurabilityStorage implements DurabilityStorage {

    private static final int FORMAT_VERSION = 1;

    private final File dataFile;

    public FlatFileDurabilityStorage(File dataFile) {
        this.dataFile = dataFile;
    }

    @Override
    public void load(DurabilityManager manager) throws IOException {
        if (!dataFile.exists()) {
            return;
        }
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(dataFile)))) {
            int version = in.readInt();
            if (version != FORMAT_VERSION) {
                throw new IOException("Version de fichier de durabilite inconnue: " + version);
            }
            int count = in.readInt();
            for (int i = 0; i < count; i++) {
                String world = in.readUTF();
                int x = in.readInt();
                int y = in.readInt();
                int z = in.readInt();
                int hits = in.readInt();
                manager.loadRawEntry(new BlockKey(world, x, y, z), hits);
            }
        } catch (EOFException e) {
            throw new IOException("Fichier de durabilite tronque/corrompu: " + dataFile.getName(), e);
        }
    }

    @Override
    public void save(DurabilityManager manager) throws IOException {
        File parent = dataFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Impossible de creer le dossier de donnees: " + parent);
        }

        File tmpFile = new File(parent, dataFile.getName() + ".tmp");
        // Copie locale figee AVANT d'ecrire le compteur : hitsTaken est un ConcurrentHashMap
        // modifie en continu par le thread principal (chaque explosion) pendant qu'une
        // sauvegarde asynchrone est en cours. Ecrire snapshot.size() puis iterer directement
        // sur la map live pourrait desynchroniser le nombre d'entrees annonce et le nombre
        // reellement ecrit si une entree est ajoutee/retiree entre les deux, corrompant le
        // fichier (champs mal alignes a la relecture). Cette copie garantit que le compteur
        // et les entrees ecrites proviennent exactement du meme instantane.
        Map<BlockKey, Integer> snapshot = new HashMap<>(manager.getRawHitsMap());

        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tmpFile)))) {
            out.writeInt(FORMAT_VERSION);
            out.writeInt(snapshot.size());
            for (Map.Entry<BlockKey, Integer> entry : snapshot.entrySet()) {
                BlockKey key = entry.getKey();
                out.writeUTF(key.getWorld());
                out.writeInt(key.getX());
                out.writeInt(key.getY());
                out.writeInt(key.getZ());
                out.writeInt(entry.getValue());
            }
        }

        try {
            Files.move(tmpFile.toPath(), dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            // Certains systemes de fichiers (notamment certains hebergeurs) ne supportent
            // pas le renommage atomique : on retombe sur un remplacement classique plutot
            // que de faire echouer toute la sauvegarde.
            Files.move(tmpFile.toPath(), dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
