<?php

namespace App\Form;

use App\Entity\Contrat;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\Form\Extension\Core\Type\DateType;
use Symfony\Component\Form\Extension\Core\Type\HiddenType;
use Symfony\Component\Form\Extension\Core\Type\IntegerType;
use Symfony\Component\Form\Extension\Core\Type\NumberType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;

class ContratType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $isAdmin = $options['is_admin'];
        $jobChoices = $options['job_choices']; // ['Job Title (Company)' => jobId, ...]

        if ($isAdmin) {
            // Admin mode: full manual control
            $builder
                ->add('clientId', IntegerType::class, [
                    'label' => 'ID Client',
                    'attr' => ['class' => 'bento-input']
                ])
                ->add('freelancerId', IntegerType::class, [
                    'label' => 'ID Freelancer',
                    'attr' => ['class' => 'bento-input']
                ])
                ->add('titre', TextType::class, [
                    'label' => 'Titre du Contrat',
                    'constraints' => [new \Symfony\Component\Validator\Constraints\NotBlank(['message' => 'Le titre est obligatoire.'])],
                    'attr' => ['class' => 'bento-input']
                ]);
        } else {
            // Regular user mode: pick from available jobs
            $builder
                ->add('clientId', HiddenType::class)
                ->add('freelancerId', HiddenType::class, [
                    'required' => false,
                ])
                ->add('titre', ChoiceType::class, [
                    'label' => 'Sélectionnez une offre',
                    'choices' => $jobChoices,
                    'placeholder' => '-- Choisir une offre d\'emploi --',
                    'mapped' => false,
                    'constraints' => [new \Symfony\Component\Validator\Constraints\NotBlank(['message' => 'Veuillez sélectionner une offre ou entrer un titre.'])],
                    'attr' => ['class' => 'bento-input']
                ]);
        }

        $builder
            ->add('description', TextareaType::class, [
                'label' => 'Description',
                'required' => true,
                'attr' => [
                    'class' => 'bento-input-textarea',
                    'rows' => 4,
                    'placeholder' => 'Décrivez les détails de la mission...',
                ]
            ])
            ->add('prix', NumberType::class, [
                'label' => 'Prix',
                'required' => true,
                'attr' => ['class' => 'bento-input']
            ])
            ->add('dateContrat', DateType::class, [
                'widget' => 'single_text',
                'label' => 'Date du contrat',
                'attr' => ['class' => 'bento-input']
            ])
            ->add('numTelephone', TextType::class, [
                'label' => 'Téléphone',
                'required' => true,
                'attr' => [
                    'class' => 'bento-input',
                    'placeholder' => 'ex: 55123456 (8 chiffres)'
                ]
            ]);
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => Contrat::class,
            'is_admin' => false,
            'job_choices' => [],
        ]);
    }
}
