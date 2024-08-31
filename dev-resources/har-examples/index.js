const hars = require('har-examples');
const fs = require('fs');

function writeHarFile(harType) {
    const jsonString = JSON.stringify(hars[harType], null, 2);
    const filename = `data/${harType}.har`;

    fs.writeFile(filename, jsonString, (err) => {
      if (err) {
        console.error(`Error writing file: ${err}`);
      } else {
        console.log(`JSON data has been saved to ${filename}`);
      }
    });
}

writeHarFile('application-form-encoded');
writeHarFile('application-json');
writeHarFile('application-zip');
writeHarFile('cookies');
writeHarFile('full');
writeHarFile('headers');
writeHarFile('https');
writeHarFile('image-png-no-filename');
writeHarFile('image-png');
writeHarFile('jsonObj-multiline');
writeHarFile('jsonObj-null-value');
writeHarFile('multipart-data-dataurl');
writeHarFile('multipart-data');
writeHarFile('multipart-file');
writeHarFile('multipart-form-data');
writeHarFile('query-encoded');
writeHarFile('query');
writeHarFile('short');
writeHarFile('text-plain');
writeHarFile('xml');
