module.exports = function(grunt) {
  grunt.initConfig({

    less : {
      development : {
        options : {
          paths : ["./less"],
          yuicompress : true
        },
        files : {
          "./public/css/main.css" : "./app/assets/stylesheets/main.less"
        }
      }
    },

    postcss : {
      options : {
        map : true,
        processors : [
          require('autoprefixer')({
            browsers : ['last 3 versions']
          })
        ]
      },
      dist : {
        src : './public/css/main.css'
      }
    },

    endline : {
      dist : {
        src : './public/css/main.css'
      }
    },

    watch : {
      options : {
        atBegin : true
      },
    	less : {
        files : "app/assets/stylesheets/*",
        tasks : ["less", "postcss", "endline"]
      }
    }

  });

  grunt.loadNpmTasks('grunt-contrib-less');
  grunt.loadNpmTasks('grunt-contrib-watch');
  grunt.loadNpmTasks('grunt-postcss');
  grunt.loadNpmTasks('grunt-endline');

  grunt.registerTask('dist', ["less", "postcss", "endline"]);

};
