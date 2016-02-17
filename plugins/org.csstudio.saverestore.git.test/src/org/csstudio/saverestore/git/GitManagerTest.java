package org.csstudio.saverestore.git;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

import org.csstudio.saverestore.DataProvider.ImportType;
import org.csstudio.saverestore.DataProviderException;
import org.csstudio.saverestore.FileUtilities;
import org.csstudio.saverestore.data.BaseLevel;
import org.csstudio.saverestore.data.BeamlineSet;
import org.csstudio.saverestore.data.BeamlineSetData;
import org.csstudio.saverestore.data.Branch;
import org.csstudio.saverestore.data.Snapshot;
import org.csstudio.saverestore.data.VSnapshot;
import org.csstudio.saverestore.git.Result.ChangeType;
import org.csstudio.ui.fx.util.Credentials;
import org.diirt.util.array.ArrayDouble;
import org.diirt.util.time.Timestamp;
import org.diirt.vtype.Alarm;
import org.diirt.vtype.AlarmSeverity;
import org.diirt.vtype.Display;
import org.diirt.vtype.Time;
import org.diirt.vtype.VType;
import org.diirt.vtype.ValueFactory;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * <code>GitManagerTest</code> tests the methods of the {@link GitManager}. The unit test creates a demo repository in
 * the user's temp folder (on the local file system) and calls different methods from the git manager to verify that
 * they return correct data.
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 *
 */
public class GitManagerTest {

    // create a repository in the temp folder
    private static File repositoryPath = new File(System.getProperty("java.io.temp"),
        "T" + (int) (Math.random() * 1000));

    // git uses seconds timestamp resolution
    private static long time = ((System.currentTimeMillis() - 3600000L * 48) / 1000L) * 1000L;

    private static Branch branch = new Branch();
    private static Branch secondBranch = new Branch("second", "second");
    private static BaseLevel branchBase = new BaseLevel(branch, "base", "base");
    private static BaseLevel branchBase2 = new BaseLevel(branch, "base2", "base2");
    private static BaseLevel secondBase = new BaseLevel(secondBranch, "secondBase", "secondBase");
    private static BeamlineSet branchBeamlineSet = new BeamlineSet(branch, Optional.of(branchBase),
        new String[] { "foo", "test.bms" }, GitDataProvider.ID);
    private static BeamlineSet branchBeamlineSet2 = new BeamlineSet(branch, Optional.of(branchBase),
        new String[] { "foo", "bar", "test2.bms" }, GitDataProvider.ID);
    private static BeamlineSet branchBeamlineSet3 = new BeamlineSet(branch, Optional.of(branchBase2),
        new String[] { "foo", "bar", "test2.bms" }, GitDataProvider.ID);
    private static BeamlineSet secondBeamlineSet = new BeamlineSet(secondBranch, Optional.of(secondBase),
        new String[] { "foo", "bar", "test2.bms" }, GitDataProvider.ID);
    private static BeamlineSet secondBranchBeamlineSet = new BeamlineSet(secondBranch,
        Optional.of(new BaseLevel(secondBranch, branchBase)), new String[] { "foo", "test.bms" }, GitDataProvider.ID);
    private static BeamlineSet secondBranchBeamlineSet2 = new BeamlineSet(secondBranch,
        Optional.of(new BaseLevel(secondBranch, branchBase)), new String[] { "foo", "bar", "test2.bms" },
        GitDataProvider.ID);
    private static BeamlineSetData branchBSD = new BeamlineSetData(branchBeamlineSet, Arrays.asList("pv1", "pv2"),
        Arrays.asList("rb1", "rb2"), Arrays.asList("50", "50"), "someDescription");
    private static BeamlineSetData branchBSD2 = new BeamlineSetData(branchBeamlineSet2, Arrays.asList("pv3", "pv4"),
        Arrays.asList("rb3", "rb4"), Arrays.asList("40", "40"), "someDescription2");
    private static BeamlineSetData branchBSD3 = new BeamlineSetData(branchBeamlineSet3, Arrays.asList("pv5", "pv6"),
        Arrays.asList("rb5", "rb6"), Arrays.asList("30", "30"), "someDescription3");
    private static BeamlineSetData secondBSD = new BeamlineSetData(secondBeamlineSet, Arrays.asList("pva", "pvb"),
        Arrays.asList("rba", "rbb"), Arrays.asList("20", "20"), "someDescription4");
    private static Snapshot branchSnapshot = new Snapshot(branchBeamlineSet, new Date(time), "sufferin succotash",
        "sylvester");
    private static Snapshot branchSnapshot2 = new Snapshot(branchBeamlineSet, new Date(time - 10000),
        "Y-y-you can't fool me. I have a high I.Q.", "porky pig");
    private static Snapshot branchSnapshot3 = new Snapshot(branchBeamlineSet, new Date(time - 20000), "Wabbit Season!",
        "daffy duck");
    private static Snapshot branchSnapshot4 = new Snapshot(branchBeamlineSet2, new Date(time - 30000),
        "What's up, doc?", "bugs bunny");
    private static Snapshot secondSnapshot = new Snapshot(secondBeamlineSet, new Date(time - 40000),
        "Be vewy vewy quiet, I'm hunting wabbits!, He-e-e-e-e!", "elmerfudd");

    private static Snapshot secondBranchSnapshot = new Snapshot(secondBranchBeamlineSet, new Date(time),
        "sufferin succotash", "sylvester");
    private static Snapshot secondBranchSnapshot2 = new Snapshot(secondBranchBeamlineSet, new Date(time - 10000),
        "Y-y-you can't fool me. I have a high I.Q.", "porky pig");
    private static Snapshot secondBranchSnapshot3 = new Snapshot(secondBranchBeamlineSet, new Date(time - 20000),
        "Wabbit Season!", "daffy duck");
    private static Snapshot secondBranchSnapshot4 = new Snapshot(secondBranchBeamlineSet, new Date(time + 10000),
        "I taught I taw a putty tat. In a succotash.", "tweety");

    private static VSnapshot branchV1 = new VSnapshot(branchSnapshot, Arrays.asList("pv1", "pv2"),
        Arrays.asList(true, true), Arrays.asList(createData(0), createData(1)), Arrays.asList("rb1", "rb2"),
        Arrays.asList(createData(0), createData(1)), Arrays.asList("50", "50"), Timestamp.of(new Date(time)));
    private static VSnapshot branchV2 = new VSnapshot(branchSnapshot2, Arrays.asList("pv1", "pv2"),
        Arrays.asList(true, true), Arrays.asList(createData(0), createData(1)), Arrays.asList("rb1", "rb2"),
        Arrays.asList(createData(0), createData(1)), Arrays.asList("50", "50"), Timestamp.of(new Date(time - 10000)));
    private static VSnapshot branchV3 = new VSnapshot(branchSnapshot3, Arrays.asList("pv1", "pv2"),
        Arrays.asList(true, true), Arrays.asList(createData(0), createData(1)), Arrays.asList("rb1", "rb2"),
        Arrays.asList(createData(0), createData(1)), Arrays.asList("50", "50"), Timestamp.of(new Date(time - 20000)));
    private static VSnapshot branchV4 = new VSnapshot(branchSnapshot4, Arrays.asList("pv3", "pv4"),
        Arrays.asList(true, true), Arrays.asList(createData(2), createData(3)), Arrays.asList("rb3", "rb4"),
        Arrays.asList(createData(2), createData(3)), Arrays.asList("40", "40"), Timestamp.of(new Date(time - 30000)));
    private static VSnapshot secondV5 = new VSnapshot(secondSnapshot, Arrays.asList("pva", "pvb"),
        Arrays.asList(true, true), Arrays.asList(createData(4), createData(5)), Arrays.asList("rba", "rbb"),
        Arrays.asList(createData(4), createData(5)), Arrays.asList("20", "20"), Timestamp.of(new Date(time - 40000)));
    private static VSnapshot secondV1 = new VSnapshot(secondBranchSnapshot4, Arrays.asList("pv1", "pv2"),
        Arrays.asList(true, true), Arrays.asList(createData(0), createData(1)), Arrays.asList("rb1", "rb2"),
        Arrays.asList(createData(0), createData(1)), Arrays.asList("50", "50"), Timestamp.of(new Date(time)));

    private static VType createData(int type) {
        Time valueTime = ValueFactory.newTime(Timestamp.of(new Date(time - 100000L)));
        Alarm alarm;
        if (Math.random() > 0.5) {
            alarm = ValueFactory.alarmNone();
        } else {
            alarm = ValueFactory.newAlarm(AlarmSeverity.MAJOR, "HIGH");
        }
        // display is irrelevant
        Display display = ValueFactory.displayNone();
        switch (type) {
            case 1:
                return ValueFactory.newVLong((long) (Math.random() * 1000), alarm, valueTime, display);
            case 2:
                return ValueFactory.newVString(String.valueOf(new Date()) + ": working", alarm, valueTime);
            case 3:
                return ValueFactory.newVEnum(1, Arrays.asList("bugs", "tweety", "sylvester"), alarm, valueTime);
            case 4:
                return ValueFactory.newVDoubleArray(
                    new ArrayDouble(Math.random(), Math.random(), Math.random(), Math.random()), alarm, valueTime,
                    display);
            case 5:
                return ValueFactory.newVStringArray(Arrays.asList("tweery", "elmer", "duffy"), alarm, valueTime);
            case 0:
            default:
                return ValueFactory.newVDouble(Math.random(), alarm, valueTime, display);
        }

    }

    private static void writeBeamlineSet(Git git, BeamlineSetData bsd, BeamlineSet set) throws Exception {
        String dpath = set.getPathAsString();
        String base = set.getBaseLevel().get().getStorageName();
        String relativePath = base + "/BeamlineSets/" + dpath;
        Path path = Paths.get(repositoryPath.getAbsolutePath(), relativePath);
        if (!Files.exists(path)) {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            path = Files.createFile(path);
        }
        String data = FileUtilities.generateBeamlineSetContent(bsd);
        Files.write(path, data.getBytes(StandardCharsets.UTF_8));

        git.add().addFilepattern(relativePath).call();
        CommitCommand command = git.commit().setMessage("unknown comment");
        command.setCommitter("bugs bunny", "bb@looney.tunes");
        command.call();
    }

    private static void writeSnapshot(Git git, VSnapshot snapshot) throws Exception {
        String dpath = snapshot.getBeamlineSet().getPathAsString();
        String base = snapshot.getBeamlineSet().getBaseLevel().get().getStorageName();
        String relativePath = base + "/Snapshots/" + dpath.replace(".bms", ".snp");
        Path path = Paths.get(repositoryPath.getAbsolutePath(), relativePath);
        if (!Files.exists(path)) {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            path = Files.createFile(path);
        }
        String data = FileUtilities.generateSnapshotFileContent(snapshot);
        Files.write(path, data.getBytes(StandardCharsets.UTF_8));

        git.add().addFilepattern(relativePath).call();
        CommitCommand command = git.commit().setMessage(snapshot.getSnapshot().get().getComment());
        command.setCommitter(new PersonIdent(snapshot.getSnapshot().get().getOwner(), "UNKNOWN@looney.tunes",
            snapshot.getSnapshot().get().getDate(), TimeZone.getTimeZone("GMT")));
        command.call();
    }

    private static void createNewBranch(Git git, Branch branch) throws Exception {
        try {
            Ref ref = null;
            try {
                ref = git.checkout().setName(branch.getShortName()).setUpstreamMode(SetupUpstreamMode.TRACK).call();
            } catch (Exception e) {
            }
            if (ref == null) {
                // local branch does not exist. create it
                git.branchCreate().setName(branch.getShortName()).call();
                git.checkout().setName(branch.getShortName()).setUpstreamMode(SetupUpstreamMode.TRACK).call();
            }
        } catch (Exception e) {
        }
    }

    @BeforeClass
    public static void createRepository() throws Exception {
        destroyRepository();
        Git git = Git.init().setDirectory(repositoryPath).call();
        // these go to the default branch (master)
        writeBeamlineSet(git, branchBSD, branchBeamlineSet);
        writeBeamlineSet(git, branchBSD2, branchBeamlineSet2);
        writeBeamlineSet(git, branchBSD3, branchBeamlineSet3);
        writeSnapshot(git, branchV4);
        writeSnapshot(git, branchV3);
        writeSnapshot(git, branchV2);
        writeSnapshot(git, branchV1);
        // create the second branch and add a beamline set
        createNewBranch(git, secondBranch);
        writeBeamlineSet(git, secondBSD, secondBeamlineSet);
        writeSnapshot(git, secondV5);
        writeSnapshot(git, secondV1);
        git.close();
    }

    @AfterClass
    public static void destroyRepository() throws InterruptedException {
        GitManager.deleteFolder(repositoryPath);
    }

    private GitManager manager;

    @Before
    public void setUp() throws Exception {
        manager = new GitManager(){
            @Override
            protected Credentials getCredentials(Optional<Credentials> previous) {
                return new Credentials("", new char[0], false);
            }
        };
        manager.initialise(repositoryPath.toURI(), repositoryPath);
        // clean up just in case
        tearDown();
    }

    @After
    public void tearDown() throws Exception {
        try (Git git = Git.open(repositoryPath)) {
            git.checkout().setName("master").call();
            Git.open(repositoryPath).branchDelete().setForce(true).setBranchNames("foobar", "foobar2").call();
        }
    }

    @Test
    public void testGetBranches() throws GitAPIException {
        List<Branch> branches = manager.getBranches();
        assertEquals(2, branches.size());
        assertEquals(branch, branches.get(0));
        assertEquals(secondBranch, branches.get(1));
    }

    @Test
    public void testGetBaseLevels() throws GitAPIException, IOException {
        List<BaseLevel> baseLevels = manager.getBaseLevels(branch);
        assertEquals(2, baseLevels.size());
        assertEquals(branchBase, baseLevels.get(0));
        assertEquals(branchBase2, baseLevels.get(1));
        baseLevels = manager.getBaseLevels(secondBranch);
        assertEquals(3, baseLevels.size());
        assertEquals(new BaseLevel(secondBranch, branchBase), baseLevels.get(0));
        assertEquals(new BaseLevel(secondBranch, branchBase2), baseLevels.get(1));
        assertEquals(new BaseLevel(secondBranch, secondBase), baseLevels.get(2));
    }

    @Test
    public void testGetBeamlineSets() throws GitAPIException, IOException {
        List<BeamlineSet> beamlineSets = manager.getBeamlineSets(Optional.of(branchBase), branch);
        assertEquals(2, beamlineSets.size());
        assertTrue(beamlineSets.contains(branchBeamlineSet));
        assertTrue(beamlineSets.contains(branchBeamlineSet2));
        // second base has only one bs
        beamlineSets = manager.getBeamlineSets(Optional.of(branchBase2), branch);
        assertEquals(1, beamlineSets.size());
        assertEquals(branchBeamlineSet3, beamlineSets.get(0));
        // there are no sets in the second base on the master branch (the base does not exist there)
        beamlineSets = manager.getBeamlineSets(Optional.of(secondBase), branch);
        assertTrue(beamlineSets.isEmpty());

        // second branch has the same number of beamline sets in the first base
        beamlineSets = manager.getBeamlineSets(Optional.of(branchBase), secondBranch);
        assertEquals(2, beamlineSets.size());
        assertTrue(beamlineSets.contains(secondBranchBeamlineSet));
        assertTrue(beamlineSets.contains(secondBranchBeamlineSet2));

        // second branch has 1 bs in the second base
        beamlineSets = manager.getBeamlineSets(Optional.of(secondBase), secondBranch);
        assertEquals(1, beamlineSets.size());
        assertEquals(secondBeamlineSet, beamlineSets.get(0));
    }

    @Test
    public void testGetSnapshots() throws GitAPIException, IOException {
        List<Snapshot> snapshots = manager.getSnapshots(branchBeamlineSet, 0, Optional.empty());
        assertEquals(3, snapshots.size());
        // parameters are not the same, because git adds the git commit hash
        assertTrue(branchSnapshot.almostEquals(snapshots.get(0)));
        assertTrue(branchSnapshot2.almostEquals(snapshots.get(1)));
        assertTrue(branchSnapshot3.almostEquals(snapshots.get(2)));

        snapshots = manager.getSnapshots(branchBeamlineSet2, 0, Optional.empty());
        assertEquals(1, snapshots.size());
        assertTrue(branchSnapshot4.almostEquals(snapshots.get(0)));

        snapshots = manager.getSnapshots(branchBeamlineSet3, 0, Optional.empty());
        assertTrue(snapshots.isEmpty());

        // the second branch has one additional snapshot for the branchBeamlineSet
        snapshots = manager.getSnapshots(secondBranchBeamlineSet, 0, Optional.empty());
        assertEquals(4, snapshots.size());
        assertTrue(secondBranchSnapshot4.almostEquals(snapshots.get(0)));
        assertTrue(secondBranchSnapshot.almostEquals(snapshots.get(1)));
        assertTrue(secondBranchSnapshot2.almostEquals(snapshots.get(2)));
        assertTrue(secondBranchSnapshot3.almostEquals(snapshots.get(3)));

        snapshots = manager.getSnapshots(secondBranchBeamlineSet, 2, Optional.of(snapshots.get(1)));
        assertEquals(2, snapshots.size());
        assertTrue(secondBranchSnapshot2.almostEquals(snapshots.get(0)));
        assertTrue(secondBranchSnapshot3.almostEquals(snapshots.get(1)));

        snapshots = manager.getSnapshots(secondBeamlineSet, 0, Optional.empty());
        assertEquals(1, snapshots.size());
        assertTrue(secondSnapshot.almostEquals(snapshots.get(0)));
    }

    @Test
    public void testGetBeamlineSetContent() throws GitAPIException, IOException {
        List<BeamlineSet> sets = manager.getBeamlineSets(Optional.of(branchBase), branch);
        for (BeamlineSet s : sets) {
            BeamlineSetData bsd = manager.loadBeamlineSetData(s, Optional.empty());
            if (bsd.getDescriptor().equals(branchBeamlineSet)) {
                assertEquals(branchBSD, bsd);
            } else if (bsd.getDescriptor().equals(branchBeamlineSet2)) {
                assertEquals(branchBSD2, bsd);
            } else {
                fail("Unknown set: " + s);
            }
        }

        sets = manager.getBeamlineSets(Optional.of(secondBase), secondBranch);
        for (BeamlineSet set : sets) {
            BeamlineSetData bsd = manager.loadBeamlineSetData(set, Optional.empty());
            if (set.equals(secondBeamlineSet)) {
                assertEquals(secondBSD, bsd);
            }
            // there might be another beamline set, which was created by #testSaveBeamlineSet
        }
    }

    @Test
    public void testGetSnapshotContent() throws GitAPIException, IOException, ParseException {
        List<Snapshot> snapshots = manager.getSnapshots(branchBeamlineSet, 0, Optional.empty());
        VSnapshot snap = manager.loadSnapshotData(snapshots.get(0));
        assertTrue(branchV1.equalsExceptSnapshot(snap));
        snap = manager.loadSnapshotData(snapshots.get(1));
        assertTrue(branchV2.equalsExceptSnapshot(snap));
        snap = manager.loadSnapshotData(snapshots.get(2));
        assertTrue(branchV3.equalsExceptSnapshot(snap));
    }

    @Test
    public void testSaveBeamlineSet() throws IOException, GitAPIException {
        BeamlineSet newBeamlineSet = new BeamlineSet(secondBranch, Optional.of(secondBase),
            new String[] { "created", "set" }, GitDataProvider.ID);
        BeamlineSetData bsd = new BeamlineSetData(newBeamlineSet, Arrays.asList("t1", "t2"), Arrays.asList("r1", "r2"),
            Arrays.asList("1", "1"), "my description");
        Result<BeamlineSetData> result = manager.saveBeamlineSet(bsd, "new comment");
        assertEquals(bsd, result.data);
        assertEquals(ChangeType.SAVE, result.change);

        List<BeamlineSet> sets = manager.getBeamlineSets(Optional.of(secondBase), secondBranch);
        boolean found = false;
        String[] path = new String[] { "created", "set.bms" };
        String[] wrongPath = new String[] { "created", "set" };
        for (BeamlineSet s : sets) {
            if (Arrays.equals(path, s.getPath())) {
                found = true;
            } else if (Arrays.equals(path, wrongPath)) {
                fail("The path should have the .bms suffix");
            }
        }
        if (!found) {
            fail("The new beamline set was not found.");
        }

        BeamlineSetData data = manager.loadBeamlineSetData(newBeamlineSet, Optional.empty());
        assertEquals(bsd, data);
    }

    @Test
    public void testSaveSnapshot() throws IOException, GitAPIException, ParseException {
        // first create the beamline set
        BeamlineSet newBeamlineSet = new BeamlineSet(secondBranch, Optional.of(secondBase),
            new String[] { "created", "set.bms" }, GitDataProvider.ID);
        BeamlineSetData bsd = new BeamlineSetData(newBeamlineSet, Arrays.asList("t1", "t2"), Arrays.asList("r1", "r2"),
            Arrays.asList("1", "1"), "my description");
        Result<BeamlineSetData> res = manager.saveBeamlineSet(bsd, "new comment");
        assertEquals(ChangeType.SAVE, res.change);
        // now save the snapshot
        String comment = "some comment";
        Date date = new Date((System.currentTimeMillis() / 1000) * 1000);
        Timestamp time = Timestamp.of(date);
        Snapshot snapshot = new Snapshot(newBeamlineSet, date, comment, "");
        VSnapshot vSnapshot = new VSnapshot(snapshot, Arrays.asList("t1", "t2"), Arrays.asList(true, false),
            Arrays.asList(createData(0), createData(0)), Arrays.asList("r1", "r2"),
            Arrays.asList(createData(0), createData(0)), Arrays.asList("1", "1"), time);

        Result<VSnapshot> result = manager.saveSnapshot(vSnapshot, comment);
        assertEquals(ChangeType.SAVE, result.change);
        assertTrue(vSnapshot.equalsExceptSnapshot(result.data));
        assertTrue(vSnapshot.getSnapshot().get().almostEquals(result.data.getSnapshot().get()));
        List<Snapshot> snapshots = manager.getSnapshots(newBeamlineSet, 0, Optional.empty());
        assertTrue(snapshot.almostEquals(snapshots.get(0)));
        VSnapshot newSnapshot = manager.loadSnapshotData(snapshots.get(0));
        assertEquals(result.data, newSnapshot);
    }

    @Test
    public void testTagSnapshot() throws IOException, GitAPIException, DataProviderException {
        List<Snapshot> snapshots = manager.getSnapshots(branchBeamlineSet, 1, Optional.empty());
        Snapshot original = snapshots.get(0);
        assertFalse(original.getTagMessage().isPresent());
        assertFalse(original.getTagName().isPresent());
        String tagName = "mytag";
        String tagMessage = "some tag message";
        Result<Snapshot> result = manager.tagSnapshot(original, tagName, tagMessage);
        assertEquals(ChangeType.SAVE, result.change);
        assertNotEquals("The original snapshot does not have the tag stuff", original, result.data);
        assertTrue("The snapshots are identical in everything except parameters (including tag stuff",
            original.almostEquals(result.data));
        snapshots = manager.getSnapshots(branchBeamlineSet, 1, Optional.empty());
        Snapshot newSnapshot = snapshots.get(0);
        assertEquals(result.data, newSnapshot);
        assertEquals(tagName, newSnapshot.getTagName().get());
        assertEquals(tagMessage, newSnapshot.getTagMessage().get());

        // remove tag
        result = manager.tagSnapshot(original, "", "");
        snapshots = manager.getSnapshots(branchBeamlineSet, 1, Optional.empty());
        newSnapshot = snapshots.get(0);
        assertFalse(original.getTagMessage().isPresent());
        assertFalse(original.getTagName().isPresent());
    }

    @Test
    public void testFindSnapshots() throws IOException, GitAPIException, DataProviderException {
        List<Snapshot> snapshots = manager.findSnapshotsByCommentOrUser("succotash", branch, true, false,
            Optional.empty(), Optional.empty());
        assertEquals(1, snapshots.size());
        snapshots = manager.findSnapshotsByCommentOrUser("succotash", secondBranch, true, false, Optional.empty(),
            Optional.empty());
        assertEquals(2, snapshots.size());
        Snapshot orgSnapshot = snapshots.get(0);

        Date start = new Date(System.currentTimeMillis() - 3600000 * 96);
        Date stop = new Date(System.currentTimeMillis() - 3600000 * 72);
        snapshots = manager.findSnapshotsByCommentOrUser("succotash", secondBranch, true, false, Optional.of(start),
            Optional.of(stop));
        assertTrue(snapshots.isEmpty());
        start = new Date(time + 1000);
        snapshots = manager.findSnapshotsByCommentOrUser("succotash", secondBranch, true, false, Optional.of(start),
            Optional.empty());
        assertEquals(1, snapshots.size());

        snapshots = manager.findSnapshotsByCommentOrUser("tweety", secondBranch, false, true, Optional.empty(),
            Optional.empty());
        assertEquals(1, snapshots.size());
        snapshots = manager.findSnapshotsByCommentOrUser("tweety", branch, false, true, Optional.empty(),
            Optional.empty());
        assertTrue(snapshots.isEmpty());

        snapshots = manager.findSnapshotsByTag("golden", secondBranch, Optional.empty(), Optional.empty());
        assertTrue(snapshots.isEmpty());

        // make a tag
        manager.tagSnapshot(orgSnapshot, "GoldenOrbit", "this is a tag message for testing");
        snapshots = manager.findSnapshotsByTag("golden", secondBranch, Optional.empty(), Optional.empty());
        assertEquals(1, snapshots.size());
        snapshots = manager.findSnapshotsByTagName("golden", secondBranch, Optional.empty(), Optional.empty());
        assertEquals(1, snapshots.size());
        snapshots = manager.findSnapshotsByTagMessage("golden", secondBranch, Optional.empty(), Optional.empty());
        assertTrue(snapshots.isEmpty());
        snapshots = manager.findSnapshotsByTagMessage("essa", secondBranch, Optional.empty(), Optional.empty());
        assertEquals(1, snapshots.size());
    }

    @Test
    public void testDeleteBeamlineSet() throws IOException, GitAPIException {
        BeamlineSet newBeamlineSet = new BeamlineSet(secondBranch, Optional.of(secondBase),
            new String[] { "created", "set2.bms" }, GitDataProvider.ID);
        BeamlineSetData bsd = new BeamlineSetData(newBeamlineSet, Arrays.asList("t1", "t2"), Arrays.asList("r1", "r2"),
            Arrays.asList("1", "1"), "my description");
        List<BeamlineSet> sets = manager.getBeamlineSets(Optional.of(secondBase), secondBranch);
        assertFalse("Beamline does not exist", sets.contains(newBeamlineSet));
        manager.saveBeamlineSet(bsd, "new comment");
        sets = manager.getBeamlineSets(Optional.of(secondBase), secondBranch);
        assertTrue("Beamline set was created", sets.contains(newBeamlineSet));
        BeamlineSetData data = manager.loadBeamlineSetData(newBeamlineSet, Optional.empty());
        assertNotNull("Beamline set has data", data);

        Result<BeamlineSet> result = manager.deleteBeamlineSet(data.getDescriptor(), "delete beamline set");
        assertEquals(ChangeType.SAVE, result.change);
        assertEquals(newBeamlineSet, result.data);
        sets = manager.getBeamlineSets(Optional.of(secondBase), secondBranch);
        assertFalse("Beamline set does not exist anymore", sets.contains(newBeamlineSet));
    }

    @Test
    public void testCreateBranch() throws GitAPIException, IOException {
        List<Branch> branches = manager.getBranches();
        assertEquals(2, branches.size());
        assertFalse(branches.contains(new Branch("foobar", "foobar")));
        Branch newBranch = manager.createBranch(branch, "foobar");
        branches = manager.getBranches();
        assertEquals(3, branches.size());
        assertTrue(branches.contains(newBranch));
        // branch is deleted in teardown
    }

    @Test
    public void testImport() throws GitAPIException, IOException, ParseException {
        // create new branch and add some data
        Branch newBranch = manager.createBranch(branch, "foobar");
        Branch newBranch2 = manager.createBranch(branch, "foobar2");
        BaseLevel bl = new BaseLevel(newBranch, "tralala", "tralala");
        BeamlineSet newBeamlineSet = new BeamlineSet(newBranch, Optional.of(bl),
            new String[] { "created", "test", "set.bms" }, GitDataProvider.ID);
        BeamlineSetData bsd = new BeamlineSetData(newBeamlineSet, Arrays.asList("t1", "t2"), Arrays.asList("r1", "r2"),
            Arrays.asList("1", "1"), "my description");
        manager.saveBeamlineSet(bsd, "some comment");
        // create two snapshots
        Snapshot snp = new Snapshot(newBeamlineSet, null, null, null);
        Timestamp firstTime = Timestamp.of(1234567, 0);
        Timestamp secondTime = Timestamp.of(System.currentTimeMillis() / 1000, 0);
        VSnapshot snapshot = new VSnapshot(snp, Arrays.asList("t1", "t2"), Arrays.asList(true, true),
            Arrays.asList(createData(0), createData(0)), Arrays.asList("r1", "r2"),
            Arrays.asList(createData(0), createData(0)), Arrays.asList("1", "2"), firstTime);
        VSnapshot snapshot2 = new VSnapshot(snp, Arrays.asList("t3", "t4"), Arrays.asList(true, true),
            Arrays.asList(createData(0), createData(0)), Arrays.asList("r3", "r4"),
            Arrays.asList(createData(0), createData(0)), Arrays.asList("1", "2"), secondTime);
        manager.saveSnapshot(snapshot, "first comment");
        manager.saveSnapshot(snapshot2, "second comment");
        List<Snapshot> snapshots = manager.getSnapshots(newBeamlineSet, 0, Optional.empty());
        assertEquals(2, snapshots.size());

        BeamlineSet newBeamlineSet2 = new BeamlineSet(newBranch, Optional.of(bl),
            new String[] { "created", "test", "set2.bms" }, GitDataProvider.ID);
        BeamlineSetData bsd2 = new BeamlineSetData(newBeamlineSet2, Arrays.asList("t1", "t2"),
            Arrays.asList("r1", "r2"), Arrays.asList("1", "1"), "my description 2");
        manager.saveBeamlineSet(bsd2, "some comment");
        // create two snapshots
        snp = new Snapshot(newBeamlineSet2, new Date(), null, null);
        VSnapshot asnapshot = new VSnapshot(snp, Arrays.asList("t1", "t2"), Arrays.asList(true, true),
            Arrays.asList(createData(0), createData(0)), Arrays.asList("r1", "r2"),
            Arrays.asList(createData(0), createData(0)), Arrays.asList("1", "2"), firstTime);
        VSnapshot asnapshot2 = new VSnapshot(snp, Arrays.asList("t3", "t4"), Arrays.asList(true, true),
            Arrays.asList(createData(0), createData(0)), Arrays.asList("r3", "r4"),
            Arrays.asList(createData(0), createData(0)), Arrays.asList("1", "2"), secondTime);
        manager.saveSnapshot(asnapshot, "first comment");
        manager.saveSnapshot(asnapshot2, "second comment");
        snapshots = manager.getSnapshots(newBeamlineSet, 0, Optional.empty());
        assertEquals(2, snapshots.size());

        BeamlineSet newBeamlineSet3 = new BeamlineSet(newBranch, Optional.of(bl),
            new String[] { "created", "test", "set3.bms" }, GitDataProvider.ID);
        BeamlineSetData bsd3 = new BeamlineSetData(newBeamlineSet3, Arrays.asList("t1", "t2"),
            Arrays.asList("r1", "r2"), Arrays.asList("1", "1"), "my description 3");
        manager.saveBeamlineSet(bsd3, "some comment");
        // create two snapshots
        snp = new Snapshot(newBeamlineSet3, new Date(), null, null);
        VSnapshot bsnapshot = new VSnapshot(snp, Arrays.asList("t1", "t2"), Arrays.asList(true, true),
            Arrays.asList(createData(0), createData(0)), Arrays.asList("r1", "r2"),
            Arrays.asList(createData(0), createData(0)), Arrays.asList("1", "2"), firstTime);
        VSnapshot bsnapshot2 = new VSnapshot(snp, Arrays.asList("t3", "t4"), Arrays.asList(true, true),
            Arrays.asList(createData(0), createData(0)), Arrays.asList("r3", "r4"),
            Arrays.asList(createData(0), createData(0)), Arrays.asList("1", "2"), secondTime);
        manager.saveSnapshot(bsnapshot, "first comment");
        manager.saveSnapshot(bsnapshot2, "second comment");
        snapshots = manager.getSnapshots(newBeamlineSet, 0, Optional.empty());
        assertEquals(2, snapshots.size());

        // *********************************************************************
        // * preparation ends here
        // *********************************************************************

        // import only beamline set
        bl = new BaseLevel(newBranch2, "tralala", "tralala");
        Result<Boolean> result = manager.importData(newBeamlineSet, newBranch2, Optional.of(bl),
            ImportType.BEAMLINE_SET);
        assertEquals(ChangeType.SAVE, result.change);
        assertTrue(result.data);
        List<BeamlineSet> sets = manager.getBeamlineSets(Optional.of(bl), newBranch2);
        assertEquals(1, sets.size());
        assertArrayEquals(newBeamlineSet.getPath(), sets.get(0).getPath());
        BeamlineSetData data = manager.loadBeamlineSetData(sets.get(0), Optional.empty());
        assertTrue(data.equalContent(bsd));
        List<Snapshot> snaps = manager.getSnapshots(sets.get(0), 0, Optional.empty());
        assertTrue("No snapshots were imported for this beamline set", snaps.isEmpty());

        // import beamline set and last snapshot
        bl = new BaseLevel(newBranch2, "tralala2", "tralala2");
        result = manager.importData(newBeamlineSet2, newBranch2, Optional.of(bl), ImportType.LAST_SNAPSHOT);
        assertEquals(ChangeType.SAVE, result.change);
        assertTrue(result.data);
        sets = manager.getBeamlineSets(Optional.of(bl), newBranch2);
        assertEquals(1, sets.size());
        assertArrayEquals(newBeamlineSet2.getPath(), sets.get(0).getPath());
        data = manager.loadBeamlineSetData(sets.get(0), Optional.empty());
        assertTrue(data.equalContent(bsd2));
        snaps = manager.getSnapshots(sets.get(0), 0, Optional.empty());
        assertEquals("Only one snapshot imported", 1, snaps.size());
        VSnapshot newData = manager.loadSnapshotData(snaps.get(0));
        assertTrue(asnapshot2.equalsExceptSnapshotOrBeamlineSet(newData));
        assertEquals("second comment", newData.getSnapshot().get().getComment());

        // import everything
        bl = new BaseLevel(newBranch2, "tralala3", "tralala3");
        result = manager.importData(newBeamlineSet3, newBranch2, Optional.of(bl), ImportType.ALL_SNAPSHOTS);
        assertEquals(ChangeType.SAVE, result.change);
        assertTrue(result.data);
        sets = manager.getBeamlineSets(Optional.of(bl), newBranch2);
        assertEquals(1, sets.size());
        assertArrayEquals(newBeamlineSet3.getPath(), sets.get(0).getPath());
        data = manager.loadBeamlineSetData(sets.get(0), Optional.empty());
        assertTrue(data.equalContent(bsd3));
        snaps = manager.getSnapshots(sets.get(0), 0, Optional.empty());
        assertEquals("Two snapshots imported", 2, snaps.size());
        newData = manager.loadSnapshotData(snaps.get(0));
        assertTrue(bsnapshot2.equalsExceptSnapshotOrBeamlineSet(newData));
        assertEquals("second comment", newData.getSnapshot().get().getComment());
        newData = manager.loadSnapshotData(snaps.get(1));
        assertTrue(bsnapshot.equalsExceptSnapshotOrBeamlineSet(newData));
        assertEquals("first comment", newData.getSnapshot().get().getComment());
    }

}