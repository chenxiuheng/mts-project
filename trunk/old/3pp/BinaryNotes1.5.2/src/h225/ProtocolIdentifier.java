
package h225;
//
// This file was generated by the BinaryNotes compiler.
// See http://bnotes.sourceforge.net 
// Any modifications to this file will be lost upon recompilation of the source ASN.1. 
//

import org.bn.*;
import org.bn.annotations.*;
import org.bn.annotations.constraints.*;
import org.bn.coders.*;
import org.bn.types.*;




    @ASN1PreparedElement
    @ASN1BoxedType ( name = "ProtocolIdentifier" )
    public class ProtocolIdentifier implements IASN1PreparedElement {
    
            @ASN1ObjectIdentifier ( name = "ProtocolIdentifier" )
            
            private ObjectIdentifier value = null;
            
            public ProtocolIdentifier() {
            }

            public ProtocolIdentifier(ObjectIdentifier value) {
                this.value = value;
            }
            
            public void setValue(ObjectIdentifier value) {
                this.value = value;
            }
            
            public ObjectIdentifier getValue() {
                return this.value;
            }

	    public void initWithDefaults() {
	    }

        private static IASN1PreparedElementData preparedData = CoderFactory.getInstance().newPreparedElementData(ProtocolIdentifier.class);
        public IASN1PreparedElementData getPreparedData() {
            return preparedData;
        }

    }
            