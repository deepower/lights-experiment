console.log("artnet test");

var options = {
    host: '192.168.88.196'
}
 
var artnet = require('artnet')(options);
 
// set channel 1 to 255 and disconnect afterwards. 
artnet.set(1, 100, function (err, res) {
    artnet.close();
});