var path = require('path');
module.exports = {
  mode: 'development',
  entry: {
    main: './src/main/resources/app/javascript/journeyplan.js',
    map: './src/main/resources/app/javascript/map.js'
  },
  output: {
    path: path.resolve(__dirname, 'src/main/resources/app/dist/'),
    filename: '[name].js',
    publicPath: '/app/dist'
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
      }
    ]
  }
};