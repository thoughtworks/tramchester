var path = require('path');
module.exports = {
  mode: 'development',
  entry: './src/main/resources/app/javascript/journeyplan.js',
  output: {
    path: path.resolve(__dirname, 'src/main/resources/app/dist/'),
    filename: 'main.js',
    publicPath: '/app/dist'
  }
};