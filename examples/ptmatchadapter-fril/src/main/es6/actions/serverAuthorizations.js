import fetch from 'isomorphic-fetch';
import postAsForm from './postAsForm';

import {checkStatus, retrieve} from './index';

export const REQUEST_SVR_AUTH = 'REQUEST_SVR_AUTH';
export const RECEIVE_SVR_AUTH = 'RECEIVE_SVR_AUTH';

const resourcePath = '/mgr/serverAuthorization';

function shouldFetchServerAuth(state) {
  const serverAuth = state.serverAuthorizations;
  console.log("in shouldFetchServerAuth");
  console.log(serverAuth);
  if (serverAuth.length === 0 && !serverAuth.isFetching) {
    return true;
  } else {
    return false;
  }
}

export function fetchServerAuthIfNeeded() {
  return (dispatch, getState) => {
    if (shouldFetchServerAuth(getState())) {
      dispatch({type: REQUEST_SVR_AUTH});
    console.log('fetchSvrAuth resource path: ' + resourcePath);
      return dispatch(retrieve(RECEIVE_SVR_AUTH, resourcePath));
    }
  };
}

export function postServerAuthAsForm(serverAuth) {
  return (dispatch) => {
    console.log("about to post form - server auth");
    var response = postAsForm('/mgr/serverAuthForm', serverAuth);
    console.log(response);
    console.log("about to get server auths");
    return dispatch(retrieve(RECEIVE_SVR_AUTH, resourcePath));
  };
}


export function createServerAuth(serverAuth) {
  return (dispatch) => {
    console.log("about to post server auth");
    fetch(resourcePath, {
      credentials: 'include',
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json'
      },
      method: 'POST',
      body: JSON.stringify(serverAuth)
    })
    .then(checkStatus)
    .catch(function(error) {
      console.error('request failed', error);
    });
    console.log("about to get server auths");
    return dispatch(retrieve(RECEIVE_SVR_AUTH, resourcePath));
  };
}
