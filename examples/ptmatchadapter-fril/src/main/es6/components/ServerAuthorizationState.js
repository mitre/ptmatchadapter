import React, {PropTypes} from 'react';

const ServerAuthorizationState = ({
  state
}) =>
<span className='state-val'>
  {(() => {
    var status = "Unknown";
    if (typeof state.expiresAt === 'undefined' || state.expiresAt == null) {
      status = "Inactive";
    } else {
      var now = Date();
      if (now > state.expiresAt) {
        status = "Expired";
      } else {
        status = "Active";
      }
    }
    return <span>{status}</span>;
  })()}

</span>;

ServerAuthorizationState.displayName = 'Server Authorization State';

ServerAuthorizationState.propTypes = {
  state: PropTypes.object.isRequired
};

export default ServerAuthorizationState;
