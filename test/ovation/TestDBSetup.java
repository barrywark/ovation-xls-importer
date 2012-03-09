package ovation;

import com.google.common.collect.Sets;
import com.objy.db.app.ooDBObj;
import ovation.license.*;
import ovation.license.LicenseUtilities;

import java.util.Iterator;
import java.util.Set;

/**
 * We use this to do the dirty work of setting up a test database
 */
public class TestDBSetup {
    public static void setupDB(String connectionFile,
                               String institution_name,
                               String lab_name,
                               String license_code,
                               String username,
                               String password) throws UserAuthenticationException {

        DataContext ctx = Ovation.connectToUnlicensedDB(connectionFile, institution_name, lab_name, license_code);

        //Setup the encryption key for the database...
        ctx.setDefaultEncryptionKey(LicenseUtilities.generateLicenseName(institution_name, lab_name));

        //License the database
        ovation.license.LicenseUtilities.licenseDatabase(ctx.getCoordinator(), institution_name, lab_name, license_code);

        try {
            ctx.authenticateUser(username,password);
        }
        catch(OvationException ex) {
            ctx.addUserUnauthenticated(username, password);
        }
    }

    public static void cleanupDB(DataContext ctx)
    {
        ctx.beginWriteTransaction();
        try {
            Iterator<ooDBObj> itr = ctx.getSession().getFD().containedDBs();
            Set<ooDBObj> toDelete = Sets.newHashSet(itr);
            for(ooDBObj db : toDelete) {
                db.delete();
            }
        }
        finally {
            ctx.commitTransaction();
        }
    }
}
