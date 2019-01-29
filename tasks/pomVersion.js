const fs = require("fs");
// version is in package.json
const packageJson = require("../package");
const newVersion = packageJson.version;
const FileHound = require("filehound");
const files = FileHound.create()
    .paths("./")
    .ext("xml")
    .match("*pom*")
    .find();
console.log(`Update all pom.xml to ${newVersion}`);
files.then(files => {
    files.forEach(file => {
        const fileName = `./${file.replace("//", "\\")}`;
        let content = fs.readFileSync(fileName, "utf8");
        content = content.replace(
            /<version>.*SNAPSHOT<\/version>/,
            `<version>${newVersion}</version>`
        );
        fs.writeFileSync(fileName, content);
    });
    console.log(`Updated ${files.length} pom.xml`);
});