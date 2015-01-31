var client_id = "336574263319-kdhnirrpvl97cqolv0ae1kjo0k49b4jh.apps.googleusercontent.com";
var scope = "https://www.googleapis.com/auth/userinfo.email";

function init() {
  var apisToLoad;
  var loadCallback = function() {
    if (--apisToLoad == 0) {
      checkAuth(function() {
        console.log("init'd...user authed...");
      });
    }
  };

  apisToLoad = 2; // must match number of calls to gapi.client.load()
  apiRoot = '//' + window.location.host + '/_ah/api';
  gapi.client.load('cloudAppDemoTool', 'v1', loadCallback, apiRoot);
  gapi.client.load('oauth2', 'v2', loadCallback);
}

function checkAuth(callback) {
  gapi.auth.authorize({client_id: client_id, scope: scope, immediate: true},
      handleAuthResult(callback));
}

function handleAuthResult(callback) {
  return function(authResult) {
    var authorizeButton = document.getElementById('authorize-button');
    if (authResult && !authResult.error) {
      authorizeButton.style.visibility = 'hidden';
      callback();
    } else {
      authorizeButton.style.visibility = '';
      authorizeButton.onclick = handleAuthClick;
    }
  };
}

function handleAuthClick(event) {
  gapi.auth.authorize({client_id: client_id, scope: scope, immediate: false}, handleAuthResult);
  return false;
}

function invokeWithUserAuth(op) {
  var request = gapi.client.oauth2.userinfo.get().execute(function(resp) {
    if (!resp.code) {
      op();
    } else {
      checkAuth(op);
    }
  });
}

function showResults(opName) {
  var firstLine = opName ? (opName + ':<br>') : '';
  return function(resp) {
      var result = (resp && resp.result) || resp;
      var text = firstLine + '<pre>' + prettyPrint(result).join('<br>') + '</pre>';
      document.getElementById('results').innerHTML = text;
  };
}

function createSingleInstanceDemo(description, callback) {
  description = description || '';
  invokeWithUserAuth(function() {
    var params = { description: description };
    gapi.client.cloudAppDemoTool.createSingleInstanceDemo(params).execute(callback, callback);
  });
}

function listActiveDemos(callback) {
  invokeWithUserAuth(function() {
    gapi.client.cloudAppDemoTool.listActiveDemos().execute(callback, callback);
  });
}

function getDemoInfo(demoId, callback) {
  invokeWithUserAuth(function() {
    var params = { demoId: demoId };
    gapi.client.cloudAppDemoTool.getDemoInfo(params).execute(callback, callback);
  });
}

function launchDemo(demoId, callback) {
  invokeWithUserAuth(function() {
    var params = { demoId: demoId };
    gapi.client.cloudAppDemoTool.launchDemo(params).execute(callback, callback);
  });
}

function teardownDemo(demoId, callback) {
  invokeWithUserAuth(function() {
    var params = { demoId: demoId };
    gapi.client.cloudAppDemoTool.teardownDemo(params).execute(callback, callback);
  });
}

function prettyPrint(obj, level, prefix, lines) {
  lines = lines || [];
  level = level || 0;
  var leadingSpaces = Array(level + 1).join(' ');
  prefix = prefix || '';
  function isArray(foo) {
    return Object.prototype.toString.call(foo) === '[object Array]';
  };
  if (typeof obj !== 'object') {
    lines.push(leadingSpaces + prefix + obj);
    return lines;
  }
  var bookends = isArray(obj) ? '[]' : '{}';
  lines.push(leadingSpaces + prefix + bookends[0]);
  for (var k in obj) {
    var nextPrefix = isArray(obj) ? '' : (k + ': ');
    prettyPrint(obj[k], level + 1, nextPrefix, lines);
  }
  lines.push(leadingSpaces + bookends[1]);
  return lines;
}

/*
function signin(mode, authorizeCallback) {
  console.log('signin invoked...');
  gapi.auth.authorize({
    client_id: "336574263319-kdhnirrpvl97cqolv0ae1kjo0k49b4jh.apps.googleusercontent.com",
    scope: "https://www.googleapis.com/auth/userinfo.email",
    immediate: mode},
    authorizeCallback);
}

function userAuthed() {
  var success = function(resp) {
    if (!resp.code) {
      console.log('logged in');
    }
  };
  var failure = function(resp) {
    console.log('login failed...response: ' + resp);
  }
  gapi.client.oauth2.userinfo.get().execute(success, failure);
}
*/
