#!/usr/bin/env node
var fs, index, main, packageJson, path, program, sourcePath;

program = require("commander");

path = require("path");

fs = require("fs");

program.option("-m", "--main", "main script location").parse(process.argv);

if (program.m != null) {
  sourcePath = path.join(process.cwd(), program.m);
} else {
  packageJson = require(path.join(process.cwd(), "./package.json"));
  if (packageJson != null) {
    index = packageJson["main"];
    sourcePath = path.join(process.cwd(), index);
  } else {
    throw new Error("No package json found!");
  }
}

main = require(sourcePath);

console.log(JSON.stringify(main.namespaces, null, 4));

main.convertSchemas();

console.log(JSON.stringify(main.namespaces, null, 4));
