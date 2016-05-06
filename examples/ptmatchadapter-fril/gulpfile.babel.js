import gulp from "gulp";
import gulpLoadPlugins from "gulp-load-plugins";
var browserify = require('browserify'); // bundles JS
var babelify = require('babelify'); // transforms to ES6
var source = require('vinyl-source-stream'); // uses conventional text streams with gulp

const PORT = process.env.PORT || 9006;

// Include gulp plugins listed in package.json
const plugins = gulpLoadPlugins({
  camelize: true, // if true, transforms hyphenated plugins names to camel case
  lazy: true, // whether the plugins should be lazy loaded on demand
  pattern: ['gulp-*', 'gulp.*'],
  replaceString: /\bgulp[\-.]/
});

var config = {
  port: PORT,
  devBaseUrl: 'http://localhost',
  paths: {
    html: './src/main/es6/*.html',
    js: './src/main/es6/**/*.js',
    jsx: './src/main/es6/**/*.jsx',
    css: [
      './src/main/es6/**/*.css'
    ],
    sass: [
      './src/main/es6/styles/**/*.scss'
    ],
    fonts: [
      './node_modules/bootstrap-sass/assets/fonts/bootstrap/*',
      './node_modules/font-awesome/fonts/*',
      './src/main/es6/fonts/**/*'
    ],
    images: './src/main/es6/images/*',
    dist: './src/main/resources/www',
    mainJs: './src/main/es6/index.js',
    externalJs: [
      './node_modules/jquery/dist/jquery.js',
      './node_modules/bootstrap-sass/assets/javascripts/bootstrap.js'
    ]
  }
};

// starts a local development server
gulp.task('connect', function() {
  return plugins.connect.server({
    root: ['./src/main/resources/www'],
    port: config.port,
    base: config.devBaseUrl,
    livereload: true,
    fallback: './src/main/resources/www/index.html'
  });
});

// moves html files to dist folder and reloads
gulp.task('html', function() {
  return gulp.src(config.paths.html)
      .pipe(gulp.dest(config.paths.dist))
      .pipe(plugins.connect.reload());
});

// bundles js files, moves them to dist folder, and reloads
gulp.task('js', function() {
  gulp.src(config.paths.externalJs)
      .pipe(gulp.dest(config.paths.dist + '/assets/scripts'))
      .pipe(plugins.connect.reload());

  return browserify({
    entries: config.paths.mainJs,
    require: config.paths.extJs,
    extensions: ['.js', '.jsx'],
    debug: true
  })
    .transform(babelify, { presets: ["es2015", "react"] }) // transforms JSX to JS & ES6
    .bundle() // combines all JS files into one
    .on('error', console.error.bind(console)) // reports errors
    .pipe(source('bundle.js')) // names bundle
    .pipe(gulp.dest(config.paths.dist + '/assets/scripts')) // destination
    .pipe(plugins.connect.reload()); // reloads browser
});

// bundles sass files, moves them to dist folder
gulp.task('sass', function() {
  return gulp.src(config.paths.sass)
    .pipe(plugins.sass().on('error', plugins.sass.logError))
    .pipe(plugins.concat('bundle.css'))
    .pipe(gulp.dest(config.paths.dist + '/assets/css'))
    .pipe(plugins.connect.reload()); // reloads browser
});

// migrates images to dist folder & publishes favicon
gulp.task('images', function() {
  gulp.src(config.paths.images)
      .pipe(gulp.dest(config.paths.dist + '/assets/images'))
      .pipe(plugins.connect.reload());

  gulp.src('./src/main/es6/favicon.ico')
      .pipe(gulp.dest(config.paths.dist));
});

// migrates fonts to a dist folder
gulp.task('fonts', function() {
  return gulp.src(config.paths.fonts)
      .pipe(gulp.dest(config.paths.dist + '/assets/fonts'))
      .pipe(plugins.connect.reload());
});

// handles js linting
gulp.task('lint', function() {
  return gulp.src(config.paths.js)
             .pipe(plugins.eslint({config: '.eslintrc'}))
             .pipe(plugins.eslint.format());
});

// opens the URL in a web browser
gulp.task('open', ['connect'], function() {
  gulp.src('./src/main/resources/www/index.html')
      .pipe(plugins.open({ uri: config.devBaseUrl + ':' + config.port + '/'}));
});

// watches files for changes and reloads
gulp.task('watch', function() {
  gulp.watch(config.paths.html, ['html']);
  gulp.watch(config.paths.js, ['js', 'lint']);
  gulp.watch(config.paths.jsx, ['js']);
  gulp.watch(config.paths.sass, ['sass']);
  gulp.watch(config.paths.fonts, ['fonts']);
});

gulp.task('default',['build', 'watch']);

// runs tasks by typing 'npm-exec gulp' in the command line
gulp.task('serve', ['html', 'js', 'sass', 'images', 'fonts', 'lint', 'open', 'watch']);
gulp.task('build', ['html', 'js', 'sass', 'images', 'fonts', 'lint']);
