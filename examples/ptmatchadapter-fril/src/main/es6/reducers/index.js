import { combineReducers } from 'redux';
import { routeReducer } from 'react-router-redux';
//import immutable from 'immutable';
import { RECEIVE_SVR_AUTH } from '../actions/serverAuthorizations';


function serverAuthorizations(state = [], action) {
  switch (action.type) {
    case RECEIVE_SVR_AUTH:
      // calling slice(0) creates a clone of the array
      return action.payload.slice(0);
    default:
      return state;
  }
}

const rootReducer = combineReducers({
  serverAuthorizations,
  routing: routeReducer
});

export default rootReducer;
