import React, { Component } from 'react';
import ServerAuthorizationList from '../components/ServerAuthorizationList';
import ServerAuthorizationForm from '../components/ServerAuthorizationForm';

class ServerAuthorizations extends Component {

  render() {
    return (
    <div className="container">
      <ServerAuthorizationList/>
      <ServerAuthorizationForm/>
    </div>
    );
  }
}

ServerAuthorizations.displayName = 'Server Authorizations DisplayName';

export default ServerAuthorizations;
