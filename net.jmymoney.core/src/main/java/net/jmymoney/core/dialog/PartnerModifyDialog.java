package net.jmymoney.core.dialog;

import java.util.Arrays;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.slf4j.Logger;

import net.jmymoney.core.component.DialogResultListener;
import net.jmymoney.core.component.DialogResultType;
import net.jmymoney.core.component.StringInputDialog;
import net.jmymoney.core.entity.SplitPartner;
import net.jmymoney.core.service.SplitPartnerService;

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
