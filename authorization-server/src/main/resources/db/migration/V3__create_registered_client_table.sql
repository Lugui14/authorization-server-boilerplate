CREATE TABLE oauth2_registered_client (
                                          id VARCHAR(255) PRIMARY KEY,
                                          client_id VARCHAR(255) NOT NULL,
                                          client_id_issued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                          client_secret VARCHAR(500),
                                          client_secret_expires_at TIMESTAMP,
                                          client_name VARCHAR(255),
                                          client_authentication_methods VARCHAR(1000) NOT NULL,
                                          authorization_grant_types VARCHAR(1000) NOT NULL,
                                          redirect_uris VARCHAR(1000),
                                          post_logout_redirect_uris VARCHAR(1000),
                                          scopes VARCHAR(1000) NOT NULL,
                                          client_settings VARCHAR(2000) NOT NULL,
                                          token_settings VARCHAR(2000) NOT NULL,
                                          CONSTRAINT uk_oauth2_registered_client_client_id UNIQUE (client_id)
);
