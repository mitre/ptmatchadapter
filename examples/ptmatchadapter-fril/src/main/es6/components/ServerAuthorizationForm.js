import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import { createServerAuth, postServerAuthAsForm } from '../actions/serverAuthorizations';

class ServerAuthorizationForm extends Component {
  constructor(props) {
    super(props);
    this.state = {title: '', description: '', serverUrl: '',
      state: '', expiresAt: ''};
  }

  handleSubmit(e) {
    e.preventDefault();
    this.props.postServerAuthAsForm(this.state);
  }

  handleFieldChange(e, field) {
    let newState = {};
    newState[field] =  e.target.value;
    this.setState(newState);
  }

  render() {
    return (
      <div className="panel panel-default">
        <div className="panel-heading">New Server Authorization</div>
        <div className="panel-body">
          <form action="/mgr/serverAuthForm" method="POST" encType='application/x-www-form-urlencoded'>
            <div className="row">
              <label>Title
                <input
                  id='title' name='title'
                  type='text'
                  placeholder='Title'
                  onChange={(e) => this.handleFieldChange(e, 'title')}
                  autoFocus
                />
              </label>
            </div>
            <div className="row">
              <label>Server URL
                <input
                  id='serverUrl' name='serverUrl'
                  type='text'
                  className='title'
                  placeholder='Server URL (required)'
                  onChange={(e) => this.handleFieldChange(e, 'serverUrl')}
                  autoFocus
                />
              </label>
            </div>
            <div className="row">
              <label>
                Description
                <textarea
                  id='description' name='description'
                  type='text'
                  placeholder='Description'
                  onChange={(e) => this.handleFieldChange(e, 'description')}
                />
              </label>
            </div>
            <div className="row">
              <input type="submit" className="btn btn-primary" name="submit" value='Create' />
              <button type="reset" value="Reset">Reset</button>
             </div>
          </form>
        </div>
      </div>
    );
  }
}

ServerAuthorizationForm.propTypes = {
  createServerAuth: PropTypes.func,
  postServerAuthAsForm: PropTypes.func
};

ServerAuthorizationForm.displayName = 'ServerAuthorizationForm DisplayName';

export default connect(null, {createServerAuth, postServerAuthAsForm})(ServerAuthorizationForm);
