const fs = require('fs');
const path = require('path');

function getAllFiles(dir, base) {
  const files = [];
  try {
    const items = fs.readdirSync(dir);
    for (const item of items) {
      const fullPath = path.join(dir, item);
      const stat = fs.statSync(fullPath);
      if (stat.isDirectory()) {
        files.push(...getAllFiles(fullPath, path.join(base, item)));
      } else if (item.endsWith('.dcm')) {
        let relPath = path.join(base, item);
        relPath = relPath.replace(/\\/g, '/');
        files.push(relPath);
      }
    }
  } catch (e) {
    console.error('Error:', e);
  }
  return files;
}

const files = getAllFiles('public/dcm', '');
files.sort();
fs.writeFileSync('public/dcm/files.json', JSON.stringify({ files }, null, 2));
console.log('Generated files.json with', files.length, 'files');
