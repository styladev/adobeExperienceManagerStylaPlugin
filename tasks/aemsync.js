/**
 * https://github.com/gavoja/aemsync
 * this piece of harmony watch the jcr_root folder for changes.
 * when grunt copy new files to the jcr_root folder, this tool push to targets
 *
 * Disable "safe write" in IntelliJ, if you have files in sync output with "___jb_old___"
 * http://stackoverflow.com/questions/23864827/jb-bak-and-jb-old-files-in-pycharm
 */
const aemsync = require("aemsync-weily"),
    exclude = "**/*.orig",// Skip merge files.
    pushInterval = 1000,
    // !!!ATTENTION this should be local for default in DEV
    deployTarget = process.env.aemSyncTarget || "local", // local | other
    withoutPublisher = process.env.withoutPublisher || false,
    jcrRoot = "src/main/content/jcr_root",
    watchDirs = [
        `./ui.apps/${jcrRoot}/`,
        `./ui.apps/${jcrRoot}/`,
        `./ui.content.styla/${jcrRoot}/`,
    ],
    targets = {
        local: [
            "http://admin:admin@localhost:4502",// author
            "http://admin:admin@localhost:4503",// publisher
        ],
    };

function aemSyncComplete() {
    // trigger livereload
    //grunt.file.write(globalConfig.tmpFolder + "/aemsyncComplete", new Date().toString());
}

function aemSyncInit() {
    const targets_ = targets[deployTarget].filter(function (target) {
            return (withoutPublisher && target.endsWith("02") || !withoutPublisher);
        }),
        onPushEnd = function (err, host) {
            if (err) {
                console.log("Error when pushing package to", arguments);
                return;
            }
            console.log("Package pushed to", host);
            aemSyncComplete();
        },
        pusher = new aemsync.Pusher(targets_, pushInterval, onPushEnd),
        watcher = new aemsync.Watcher();

    console.log("Targets are:\n", JSON.stringify(targets_, null, "\t"));
    console.log("Watch folder:\n", JSON.stringify(watchDirs, null, "\t"));

    // Initialize queue processing.
    pusher.start();

    function pushItem(localPath) {
        pusher.addItem(localPath);
    }

    // Watch over dirs
    watchDirs.forEach(function (dir) {
        watcher
            .watch(dir, exclude, pushItem);
    });
}

aemSyncInit();
