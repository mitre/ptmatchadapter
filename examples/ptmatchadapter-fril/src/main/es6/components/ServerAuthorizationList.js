import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import { fetchServerAuthIfNeeded } from '../actions/serverAuthorizations';
import  ServerAuthorizationListItem from './ServerAuthorizationListItem';
//{this.props.serverAuthorizations.map(obj => <li>{obj.title}</li>)}
//{this.props.serverAuthorizations.map((o, i) => <ServerAuthorizationListItem item={o} key={i}/>)}

class ServerAuthorizationList extends Component {
  componentDidMount() {
    this.props.fetchServerAuthIfNeeded();
  }

  render() {
    return (
      <div className="panel panel-default">
        <div className="panel-heading">
          <h3 className="panel-title">Server Authorizations</h3>
        </div>
        <div className="panel-body">
          <table>
          <thead>
            <tr>
              <th>Title</th>
              <th>Server URL</th>
              <th>Scope</th>
              <th>Authorization State</th>
              <th>Expiration</th>
            </tr>
          </thead>
          <tbody>
            {this.props.serverAuthorizations.map((o, i) => <ServerAuthorizationListItem item={o} key={i}/>)}
          </tbody>
        </table>
        </div>
      </div>
    );
  }
}

ServerAuthorizationList.displayName = 'Server Authorization List';

ServerAuthorizationList.propTypes = {
  serverAuthorizations: PropTypes.array.isRequired,
  fetchServerAuthIfNeeded: PropTypes.func
};

const mapStateToProps = (state) => {
  var props = {};
  if (state.serverAuthorizations) {
    props.serverAuthorizations = state.serverAuthorizations;
  } else {
    props.serverAuthorizations = [];
  }
  return props;
};

export default connect(mapStateToProps, { fetchServerAuthIfNeeded })(ServerAuthorizationList);
