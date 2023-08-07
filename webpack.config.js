const path = require('path');
const { VueLoaderPlugin } = require('vue-loader')

module.exports = {
  mode: 'development',
  entry: {
    main: './src/main/resources/app/javascript/journeyplan.js',
    trammap: './src/main/resources/app/javascript/trammap.js',
    traveltimes: './src/main/resources/app/javascript/traveltimes.js',
    routemap: './src/main/resources/app/javascript/routemap.js',
    linksmap: './src/main/resources/app/javascript/linksmap.js',
    frequency: './src/main/resources/app/javascript/frequency.js'
  },
  output: {
    path: path.resolve(__dirname, 'build/resources/main/app'),
    filename: '[name].js',
    publicPath: '/app/'
  },
  resolve: {
    alias: {
      vue: 'vue/dist/vue.min.js'
    }
  },
  module:{
    rules:[
      {
            test:/\.css$/,
            use:['style-loader','css-loader']
      },
      {
          test: /\.vue$/,
          loader: 'vue-loader'
      },
      {
          test:/\.png$/,
          type: 'asset/resource',
          generator: {
               filename: 'images/[hash][ext][query]'
             }
      },
      {
         test: /\.(woff|woff2|eot|ttf|otf)$/i,
         type: 'asset/resource',
         generator: {
                filename: 'fonts/[name][ext][query]'
              }
      },
      {
          test: /\.s(c|a)ss$/,
              use: [
                'vue-style-loader',
                'css-loader',
                {
                  loader: 'sass-loader',
                  // Requires sass-loader@^7.0.0
                  options: {
                    implementation: require('sass'),
                    indentedSyntax: true // optional
                  },
                  // Requires >= sass-loader@^8.0.0
                  options: {
                    implementation: require('sass'),
                    sassOptions: {
                      indentedSyntax: true // optional
                    },
                  },
                },
              ],
        },
    ]
  },
  plugins: [
      new VueLoaderPlugin()
    ]
};