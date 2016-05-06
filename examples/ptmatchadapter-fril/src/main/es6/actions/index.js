import fetch from 'isomorphic-fetch';

function receiveResponse(actionType, json) {
 return {
   type: actionType,
    payload: json
  };
}

export function retrieve(actionType, url) {
  return dispatch => {
    console.log('retrieve: ' + url);
    return fetch(url, {credentials: 'same-origin'})
      .then(resp => resp.json())
      .then(json => dispatch(receiveResponse(actionType, json)));
  };
}

export function checkStatus(response) {
  if (response.status >= 200 && response.status < 300) {
    return response;
  } else {
    var error = new Error(response.statusText);
    error.response = response;
    throw error;
  }
}
