


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
  url: 'http://localhost:8080/api/stations'
})
  .then(function (response) {
    this.posts = response.data
  });
