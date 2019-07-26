package org.eclipse.kura.security;

import org.eclipse.kura.KuraException;

/**
 * @since 2.2
 */
public interface LoginProtectionService {
    
    public void setStatus(boolean status) throws KuraException;
    
    public boolean isEnabled() throws KuraException;

}
