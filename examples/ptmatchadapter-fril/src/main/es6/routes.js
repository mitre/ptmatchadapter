import React from 'react';
import { Route } from 'react-router';

import App from './containers/App';
import ServerAuthorizations from './containers/ServerAuthorizations';

export default (
  <Route component={App}>
    <Route path="/" component={ServerAuthorizations} />
    <Route path="/index.html" component={ServerAuthorizations} />
  </Route>
);
