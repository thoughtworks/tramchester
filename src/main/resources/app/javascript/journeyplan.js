


var app = new Vue({
    el: '#journeyplan',
    data: {
        message: 'Hello journeyplan!',
    items: [
        { message: 'Foo' },
        { message: 'Bar' }
      ]
    }
});

axios({
  method: 'get',
  url: 'http://localhost/api/stations/api:8080'
})
  .then(function (response) {
    this.posts = response.data
  });
