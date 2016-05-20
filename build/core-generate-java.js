#!/usr/bin/env node
var action, actionClassName, actionName, apiConfigFile, convertSchema, ensure, fs, getVarType, i, index, javaPackage, javaPath, makeClassName, name, namespaceClassName, noParams, ns, packageJson, paramsClass, path, program, ref, ref1, ref2, replacing, resultClass, schema, schemaName, sourcePath;

program = require("commander");

path = require("path");

fs = require("fs");

ensure = require("is_js");

program.option("-m", "--main", "main script location").parse(process.argv);

if (program.m != null) {
  sourcePath = path.join(process.cwd(), program.m);
} else {
  packageJson = require(path.join(process.cwd(), "./package.json"));
  if (packageJson != null) {
    index = packageJson["main"];
    sourcePath = path.join(process.cwd(), index);
    javaPath = packageJson["javaPath"];
    if (javaPath != null) {
      javaPath = path.join(process.cwd(), javaPath);
    }
    console.log("Java path: " + javaPath);
    if (packageJson.javaPackage != null) {
      javaPackage = "package  " + packageJson.javaPackage + ";";
    } else {
      javaPackage = "//TODO: Specify package";
    }
    javaPackage += "\n";
  } else {
    throw new Error("No package json found!");
  }
}

program = require(sourcePath);

makeClassName = function(string) {
  return string.charAt(0).toUpperCase() + string.substring(1);
};

getVarType = function(options) {
  if (options.useClass != null) {
    return makeClassName(options.useClass);
  }
  if (ensure.string(options)) {
    return makeClassName(options + "Schema");
  }
  switch (options.type) {
    case "number":
      if (options.useInt) {
        return "int";
      } else {
        return "long";
      }
      break;
    case "boolean":
      return "boolean";
    case "string":
      return "String";
    case "array":
      if (options.mode === "unique") {
        throw new Error("Could not convert list with unique mode. Not supported in JAVA!");
      } else {
        return "List<" + (getVarType(options.children)) + ">";
      }
      break;
    case "object":
      if (ensure.string(options.children)) {
        return makeClassName(options.children + "Schema");
      }
      throw new Error("Unable to transform object, use a schema");
      break;
    default:
      if (options.useType != null) {
        return options.useType;
      } else {
        console.error(options);
        throw new Error("Unable to transform type, use a useType annotation");
      }
  }
};

convertSchema = function(config, indent) {
  var name, options, result, varType;
  result = "";
  if (ensure.string(config)) {
    return makeClassName(config + "Schema");
  }
  for (name in config) {
    options = config[name];
    varType = getVarType(options);
    result += indent + "public " + varType + " " + name + ";\n";
  }
  return result;
};

if ((program.isFloodProcessor != null) && program.isFloodProcessor() || true) {
  apiConfigFile = fs.readFileSync(path.join(__dirname, "../data/ApiConfig.java")).toString();
  replacing = "";
  i = "    ";
  ref = program.namespaces;
  for (name in ref) {
    ns = ref[name];
    namespaceClassName = makeClassName(name);
    replacing += i + ("public static class " + namespaceClassName + "{\n");
    ref1 = ns.schemas;
    for (schemaName in ref1) {
      schema = ref1[schemaName];
      replacing += i + i + "public static class " + makeClassName(schemaName) + "Schema{\n";
      replacing += convertSchema(schema, i + i + i);
      replacing += i + i + "}\n\n";
    }
    ref2 = ns.actions;
    for (actionName in ref2) {
      action = ref2[actionName];
      if (!action.skipJava) {
        actionClassName = makeClassName(actionName);
        if (!ensure.string(action.params)) {
          if (Object.keys(action.params).length !== 0) {
            noParams = false;
            paramsClass = actionClassName + "Params";
            replacing += i + i + "public static class " + paramsClass + "{\n";
            replacing += convertSchema(action.params, i + i + i);
            replacing += i + i + "}\n\n";
          } else {
            noParams = true;
          }
        } else {
          noParams = false;
          paramsClass = makeClassName(action.params + "Schema");
        }
        if (!ensure.string(action.result)) {
          resultClass = actionClassName + "Result";
          replacing += i + i + "public static class " + resultClass + "{\n";
          replacing += convertSchema(action.result, i + i + i);
          replacing += "" + i + i + "}\n\n";
        } else {
          resultClass = makeClassName(action.result + "Schema");
        }
        if (!noParams) {
          replacing += i + i + ("public static Request<" + resultClass + "> " + actionName + "(" + paramsClass + " params, final ActionCallback<" + resultClass + "> callback){\n");
        } else {
          replacing += i + i + ("public static Request<" + resultClass + "> " + actionName + "(final ActionCallback<" + resultClass + "> callback){\n");
        }
        replacing += i + i + i + ("return new Request<" + resultClass + ">(\"" + name + "\",\"" + actionName + "\", new Request.RequestCallback(){\n");
        replacing += i + i + i + i + "@Override\n";
        replacing += i + i + i + i + "public void onResult(String result){\n";
        replacing += i + i + i + i + i + ("callback.onResult(gson.fromJson(result," + resultClass + ".class));\n");
        replacing += i + i + i + i + i + "}\n";
        replacing += i + i + i + i + i + "@Override\n";
        replacing += i + i + i + i + i + "public void onError(String error){callback.onError(error);}\n";
        if (noParams) {
          replacing += i + i + i + "});\n";
        } else {
          replacing += i + i + i + "},params);\n";
        }
        replacing += i + i + "}\n\n";
      }
    }
    replacing += i + "}\n";
  }
  if (javaPath == null) {
    if (!fs.existsSync(path.join(process.cwd(), "./generated/"))) {
      fs.mkdirSync(path.join(process.cwd(), "./generated/"));
      javaPath = path.join(process.cwd(), "./generated/");
    }
  }
  fs.writeFileSync(javaPath + "ApiConfig.java", javaPackage + apiConfigFile.replace("//REPLACEME//", replacing));
  fs.writeFileSync(javaPath + "ApiBase.java", javaPackage + fs.readFileSync(path.join(__dirname, "../data/ApiBase.java")).toString());
  fs.writeFileSync(javaPath + "Request.java", javaPackage + fs.readFileSync(path.join(__dirname, "../data/Request.java")).toString());
}
