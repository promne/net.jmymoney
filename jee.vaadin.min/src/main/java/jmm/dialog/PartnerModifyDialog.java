package jmm.dialog;

import java.util.Arrays;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.slf4j.Logger;

import jmm.component.DialogResultListener;
import jmm.component.DialogResultType;
import jmm.component.StringInputDialog;
import jmm.entity.SplitPartner;
import jmm.service.SplitPartnerService;

@RequestScoped
public class PartnerModifyDialog {

    @Inject
    private Logger log;
    
    @Inject
    private SplitPartnerService splitPartnerService;

    @PostConstruct
    private void init() {
        log.info("Initiated");
    }
    
    public void renamePartner(SplitPartner partner, DialogResultListener... listeners) {
        StringInputDialog accountNameDialog = new StringInputDialog("Edit partner", "Enter a name of the partner");
        accountNameDialog.setValue(partner.getName());
        accountNameDialog.setDialogResultListener((closeType, resultValue) -> {
                if (DialogResultType.OK.equals(closeType)) {
                    partner.setName((String) resultValue);
                    splitPartnerService.update(partner);
                }
                Arrays.stream(listeners).forEach(s -> s.dialogClosed(closeType, resultValue));
        });
        accountNameDialog.show();
    }
}
