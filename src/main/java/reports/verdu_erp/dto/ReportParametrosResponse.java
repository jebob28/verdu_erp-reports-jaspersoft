/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package reports.verdu_erp.dto;

import java.time.LocalDateTime;
import lombok.Data;



/**
 *
 * @author jeffersontadeuleite
 */
@Data
public class ReportParametrosResponse {
    private Long id;
    private String codigo;
    private String parametros;
    private LocalDateTime criadoem;
    private LocalDateTime atualizadoem;
    private Boolean ativo;


}
