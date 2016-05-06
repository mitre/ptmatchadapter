import React, { Component, PropTypes } from 'react';
import ServerAuthorizationState from './ServerAuthorizationState'

var dateFormat = require('dateformat')

class ServerAuthorizationListItem extends Component {
  componentDidMount() {
  }

  render() {
    return (
      <tr>
        <td>{this.props.item.title}</td>
        <td><a href={this.props.item.serverUrl}>{this.props.item.serverUrl}</a></td>
        <td>{this.props.item.scope}</td>
        {(() => {
          return <td><ServerAuthorizationState state={{expiresAt: this.props.item.expiresAt, id: this.props.item.id} || {expiresAt: '', id: this.props.item.id}}/></td>
        })()}
        {(() => {
          if (typeof this.props.item.expiresAt !== 'undefined' && this.props.item.expiresAt != null) {
          
            return <td><span>{dateFormat(this.props.item.expiresAt, "isoDateTime")}</span></td>
          } else {
            return <td><span>--</span></td>
          }
        })()}
      </tr>
    );
  }
}

ServerAuthorizationListItem.displayName = 'Server Authorization List Item';

ServerAuthorizationListItem.propTypes = {
  item: PropTypes.object.isRequired
};


export default ServerAuthorizationListItem;
