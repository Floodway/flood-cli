###	
	Floodway/flood-cli GULPFILE

###

gulp 	= require("gulp")
coffee 	= require("gulp-coffee")
banner = require("gulp-banner")

# Thank you for considering to contribute. This file is used to compile files in the src directory. 

#  Directories
srcDir = "./src/**/*.coffee"
buildDir = "./build"

gulp.task("watch", ->
	gulp.watch(srcDir,["coffee"])
)

gulp.task("coffee", ->
	gulp.src(srcDir)
		.pipe(coffee(
			bare: true
		))
		.pipe(banner("#!/usr/bin/env node\n",
			pkg: require("./package.json")
		))
		.pipe(gulp.dest(buildDir))
)

gulp.task("default",["watch"])

